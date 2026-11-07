// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import com.rosan.installer.domain.share.model.LocalShareProtocol
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

/**
 * In-memory registry of authenticated dashboard sessions.
 *
 * Sessions are minted from [SecureRandom] after a correct PIN, never derived from the PIN itself.
 * A deterministic cookie made an offline brute force of the PIN possible straight from the cookie
 * value, and it also meant a leaked cookie could never be revoked without changing the PIN.
 */
internal class LocalShareSessionStore(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
    private val maxSessions: Int = MAX_SESSIONS,
) {
    private val sessions = object : LinkedHashMap<String, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean = size > maxSessions
    }

    /** Issues a fresh 128-bit session token and returns it. */
    fun issue(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        synchronized(sessions) {
            sessions[token] = clock() + ttlMillis
        }
        return token
    }

    fun isValid(token: String?): Boolean {
        if (token.isNullOrEmpty()) return false
        synchronized(sessions) {
            val now = clock()
            sessions.entries.removeAll { it.value <= now }
            val expiresAt = sessions[token]
            // Touch the entry so the LRU ordering follows recent use rather than insertion.
            if (expiresAt != null) sessions[token] = expiresAt
            return expiresAt != null
        }
    }

    fun clear() = synchronized(sessions) { sessions.clear() }

    private companion object {
        const val TOKEN_BYTES = 16
        const val MAX_SESSIONS = 64
        const val DEFAULT_TTL_MILLIS = 12L * 60 * 60 * 1000
    }
}

/**
 * Minimal HTTP/1.1 server used only on the local network. It deliberately avoids a server
 * library dependency: a dashboard page, a PIN login, a status endpoint and a streaming multipart
 * upload are all that is required.
 *
 * Security model:
 *  * only RFC1918/loopback/link-local peers may connect at all ([LocalShareProtocol.isLocalPeer]),
 *  * every `POST` carrying an `Origin`/`Referer` must be same-origin (blocks cross-site form posts
 *    and `fetch()` calls from any other page, including when the PIN is disabled),
 *  * an authenticated browser session must also present the per-run CSRF token on uploads,
 *  * the rate limiter is keyed on the socket address, never on client-supplied headers.
 */
class LocalHttpServer(
    val port: Int,
    private val pin: String,
    private val receiveDirectory: File,
    private val versionName: String,
    private val onFileReceived: (File) -> Unit,
    private val onError: (String) -> Unit,
    private val maxUploadBytes: Long = LocalShareProtocol.MAX_UPLOAD_BYTES,
    private val networkAllowed: () -> Boolean = { true },
) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newFixedThreadPool(8)
    private var serverSocket: ServerSocket? = null
    private val loginLock = Any()
    private val sessions = LocalShareSessionStore()

    /** Random per-run CSRF token embedded in the dashboard and required on browser uploads. */
    private val csrfToken: String = SecureRandom().let { random ->
        ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) }
    }

    /**
     * Bounded LRU of recently seen socket addresses. Keying on a client-supplied header let a
     * remote peer rotate the key and bypass the limiter, or flood the map with `unknown`.
     */
    private val loginAttempts = object : LinkedHashMap<String, MutableList<Long>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MutableList<Long>>): Boolean = size > MAX_RATE_LIMIT_KEYS
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        receiveDirectory.mkdirs()
        serverSocket = ServerSocket(port, 32, InetAddress.getByName("0.0.0.0"))
        executor.execute { acceptLoop() }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        sessions.clear()
        runCatching { serverSocket?.close() }
        serverSocket = null
        executor.shutdownNow()
    }

    private fun acceptLoop() {
        while (running.get()) {
            val socket = try {
                serverSocket?.accept() ?: break
            } catch (_: SocketException) {
                break
            } catch (error: Exception) {
                onError(error.message.orEmpty())
                break
            }
            if (!LocalShareProtocol.isLocalPeer(socket.inetAddress.hostAddress.orEmpty())) {
                runCatching { socket.close() }
                continue
            }
            executor.execute { handle(socket) }
        }
    }

    private fun handle(socket: Socket) {
        try {
            socket.use { connection ->
                connection.soTimeout = 60_000
                val input = BufferedInputStream(connection.getInputStream())
                val output = connection.getOutputStream()
                val requestLine = readAsciiLine(input) ?: return
                val parts = requestLine.split(' ')
                if (parts.size < 2) return
                val method = parts[0].uppercase()
                val target = parts[1]
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = readAsciiLine(input) ?: break
                    if (line.isEmpty()) break
                    val separator = line.indexOf(':')
                    if (separator > 0) headers[line.substring(0, separator).lowercase()] = line.substring(separator + 1).trim()
                }
                val path = target.substringBefore('?')
                val query = target.substringAfter('?', "")
                dispatch(
                    method = method,
                    path = path,
                    query = query,
                    headers = headers,
                    input = input,
                    output = output,
                    clientIp = socket.inetAddress.hostAddress.orEmpty(),
                )
            }
        } catch (error: Exception) {
            Timber.w(error, "Local sharing request failed")
            onError(error.message.orEmpty())
        }
    }

    private fun dispatch(
        method: String,
        path: String,
        query: String,
        headers: Map<String, String>,
        input: BufferedInputStream,
        output: OutputStream,
        clientIp: String,
    ) {
        if (!networkAllowed()) {
            respondJson(
                output,
                """{"ok":false,"error":"Network access is disabled in settings"}""",
                503,
                "Service Unavailable",
            )
            return
        }

        // Cross-site request forgery protection for *every* state-changing request, not just
        // /login. Non-browser clients (curl, the Send-to-TV uploader) send no Origin header and
        // cannot be driven by a hostile page, so they are not affected.
        if (method == "POST" && !isSameOriginRequest(headers)) {
            respondJson(output, """{"ok":false,"error":"CSRF"}""", 403, "Forbidden")
            return
        }

        val cookieToken = headers["cookie"]
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("${LocalShareProtocol.PIN_COOKIE}=") }
            ?.substringAfter('=')
        val suppliedPin = queryPin(query) ?: headers[LocalShareProtocol.PIN_HEADER.lowercase()]
        val hasSession = sessions.isValid(cookieToken)
        val authorized = pin.isEmpty() ||
            hasSession ||
            LocalShareProtocol.matchesPin(pin, suppliedPin)

        when {
            method == "POST" && path == "/login" -> {
                if (isRateLimited(clientIp)) {
                    respondJson(output, """{"ok":false,"error":"Too many attempts"}""", 429, "Too Many Requests")
                    return
                }
                val body = readBody(input, headers)
                val enteredPin = decodeFormValue(body.substringAfter("pin=", "").substringBefore('&'))
                if (LocalShareProtocol.matchesPin(pin, enteredPin)) {
                    val token = sessions.issue()
                    respond(
                        output,
                        303,
                        "See Other",
                        "text/plain",
                        "ok".toByteArray(),
                        "Set-Cookie: ${LocalShareProtocol.PIN_COOKIE}=$token; Path=/; HttpOnly; SameSite=Lax\r\n" +
                            "Location: /\r\n",
                    )
                } else {
                    recordLoginAttempt(clientIp)
                    respondHtml(output, 401, "Unauthorized", LocalShareDashboard.loginPage(error = true))
                }
            }

            !authorized -> respondHtml(output, 401, "Unauthorized", LocalShareDashboard.loginPage())

            method == "GET" && (path == "/" || path == "/index.html") ->
                respondHtml(
                    output,
                    200,
                    "OK",
                    LocalShareDashboard.mainPage(pin.isNotEmpty(), versionName, csrfToken),
                )

            method == "GET" && path == "/api/status" -> {
                val files = receiveDirectory.listFiles()?.count { it.isFile } ?: 0
                respondJson(output, """{"running":true,"files":$files,"pin":${pin.isNotEmpty()}}""")
            }

            method == "GET" && path == "/api/files" -> {
                val names = receiveDirectory.listFiles()
                    ?.filter { it.isFile }
                    ?.sortedByDescending { it.lastModified() }
                    ?.take(50)
                    ?.joinToString(",") { "\"${jsonEscape(it.name)}\"" }
                    .orEmpty()
                respondJson(output, """{"files":[$names]}""")
            }

            method == "POST" && (path == LocalShareProtocol.UPLOAD_PATH || path == "/push") -> {
                // A browser session must prove it can read the dashboard (the CSRF token) before
                // an upload is accepted; otherwise any page could POST into this device's session.
                if (hasSession && headers[CSRF_HEADER].orEmpty() != csrfToken) {
                    respondJson(output, """{"ok":false,"error":"CSRF"}""", 403, "Forbidden")
                    return
                }
                when (val result = receiveUpload(input, headers)) {
                    is UploadResult.Success -> {
                        onFileReceived(result.file)
                        respondJson(output, """{"ok":true,"name":"${jsonEscape(result.file.name)}"}""")
                    }

                    UploadResult.TooLarge -> respondJson(
                        output,
                        """{"ok":false,"error":"File exceeds the maximum upload size"}""",
                        413,
                        "Payload Too Large",
                    )

                    UploadResult.LengthRequired -> respondJson(
                        output,
                        """{"ok":false,"error":"Content-Length is required"}""",
                        411,
                        "Length Required",
                    )

                    UploadResult.Invalid -> respondJson(
                        output,
                        """{"ok":false,"error":"No file received"}""",
                        400,
                        "Bad Request",
                    )
                }
            }

            else -> respondHtml(output, 404, "Not Found", "<h1>404</h1>")
        }
    }

    private sealed interface UploadResult {
        data class Success(val file: File) : UploadResult
        data object TooLarge : UploadResult
        data object LengthRequired : UploadResult
        data object Invalid : UploadResult
    }

    private fun receiveUpload(input: BufferedInputStream, headers: Map<String, String>): UploadResult {
        val isChunked = headers["transfer-encoding"]?.lowercase()?.contains("chunked") == true
        // Chunked bodies are rejected instead of being half-decoded: a partially written upload is
        // indistinguishable from a truncated one, and every supported client knows its length.
        if (isChunked) return UploadResult.LengthRequired
        val contentLength = headers["content-length"]?.toLongOrNull() ?: return UploadResult.LengthRequired
        if (contentLength > maxUploadBytes) return UploadResult.TooLarge
        val contentType = headers["content-type"].orEmpty()
        val boundary = Regex("""boundary="?([^";]+)"?""", RegexOption.IGNORE_CASE)
            .find(contentType)?.groupValues?.get(1) ?: return UploadResult.Invalid

        var fileName: String? = null
        var headerLines = 0
        while (headerLines < 40) {
            headerLines++
            val line = readAsciiLine(input) ?: return UploadResult.Invalid
            if (line.contains("filename=", ignoreCase = true)) {
                fileName = Regex("""filename="?([^";]*)"?""", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.get(1)
            }
            if (line.isEmpty()) break
        }
        val safeName = sanitizeFileName(fileName) ?: return UploadResult.Invalid
        val marker = "\r\n--$boundary".toByteArray(Charsets.US_ASCII)
        // A shared name like "<file>.part" let two concurrent uploads of the same file truncate
        // each other. createTempFile gives every transfer its own inode.
        val tempFile = runCatching {
            File.createTempFile("upload-", ".part", receiveDirectory)
        }.getOrElse {
            onError(it.message.orEmpty())
            return UploadResult.Invalid
        }
        val target = File(receiveDirectory, safeName)
        val pending = ByteArray(marker.size)
        var pendingLength = 0
        var written = 0L
        var exceeded = false
        var matched = false
        val chunk = ByteArray(64 * 1024)

        try {
            tempFile.outputStream().buffered().use { output ->
                scan@ while (!matched) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    var index = 0
                    while (index < read) {
                        val byte = chunk[index]
                        index++
                        if (byte == marker[pendingLength]) {
                            pending[pendingLength++] = byte
                            if (pendingLength == marker.size) {
                                matched = true
                                break@scan
                            }
                        } else {
                            if (pendingLength > 0) {
                                if (written + pendingLength > maxUploadBytes) {
                                    exceeded = true
                                    break@scan
                                }
                                output.write(pending, 0, pendingLength)
                                written += pendingLength
                                pendingLength = 0
                            }
                            if (byte == marker[0]) {
                                pending[pendingLength++] = byte
                            } else {
                                if (written + 1 > maxUploadBytes) {
                                    exceeded = true
                                    break@scan
                                }
                                output.write(byte.toInt())
                                written++
                            }
                        }
                    }
                }
                if (!exceeded && !matched && pendingLength > 0) {
                    if (written + pendingLength > maxUploadBytes) {
                        exceeded = true
                    } else {
                        output.write(pending, 0, pendingLength)
                        written += pendingLength
                    }
                }
            }
        } catch (error: Exception) {
            tempFile.delete()
            onError(error.message.orEmpty())
            return UploadResult.Invalid
        }

        if (exceeded) {
            tempFile.delete()
            return UploadResult.TooLarge
        }
        if (!matched || written <= 0L) {
            tempFile.delete()
            return UploadResult.Invalid
        }
        if (!tempFile.renameTo(target)) {
            tempFile.delete()
            return UploadResult.Invalid
        }
        return UploadResult.Success(target)
    }

    /** Percent-decodes the `pin` query parameter so an encoded digit is not rejected. */
    private fun queryPin(query: String): String? = query.split('&')
        .firstOrNull { it.substringBefore('=') == "pin" }
        ?.substringAfter('=')
        ?.let(::decodeFormValue)

    private fun decodeFormValue(value: String): String = runCatching {
        java.net.URLDecoder.decode(value, "UTF-8")
    }.getOrDefault(value)

    private fun readBody(input: BufferedInputStream, headers: Map<String, String>): String {
        val length = headers["content-length"]?.toIntOrNull()?.coerceIn(0, 4096) ?: return ""
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val count = input.read(body, read, length - read)
            if (count < 0) break
            read += count
        }
        return String(body, 0, read, Charsets.UTF_8)
    }

    private fun readAsciiLine(input: BufferedInputStream): String? {
        val bytes = ByteArrayOutputStream(128)
        while (true) {
            val value = input.read()
            if (value < 0) return if (bytes.size() == 0) null else bytes.toString("UTF-8")
            if (value == '\n'.code) {
                val raw = bytes.toByteArray()
                val length = if (raw.isNotEmpty() && raw.last() == '\r'.code.toByte()) raw.size - 1 else raw.size
                return String(raw, 0, length, Charsets.UTF_8)
            }
            bytes.write(value)
            if (bytes.size() > 16 * 1024) return null
        }
    }

    private fun respondHtml(output: OutputStream, code: Int, reason: String, html: String) = respond(output, code, reason, "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))

    private fun respondJson(output: OutputStream, json: String, code: Int = 200, reason: String = "OK") = respond(output, code, reason, "application/json; charset=utf-8", json.toByteArray(Charsets.UTF_8))

    private fun respond(
        output: OutputStream,
        code: Int,
        reason: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: String = "",
    ) {
        val header = buildString {
            append("HTTP/1.1 $code $reason\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n")
            append(extraHeaders)
            append("\r\n")
        }
        output.write(header.toByteArray(Charsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    private fun isRateLimited(ip: String): Boolean = synchronized(loginLock) {
        val now = System.currentTimeMillis()
        val attempts = loginAttempts.getOrPut(ip) { mutableListOf() }
        attempts.removeAll { now - it > 60_000 }
        attempts.size >= MAX_LOGIN_ATTEMPTS
    }

    private fun recordLoginAttempt(ip: String) = synchronized(loginLock) {
        val now = System.currentTimeMillis()
        val attempts = loginAttempts.getOrPut(ip) { mutableListOf() }
        attempts.removeAll { now - it > 60_000 }
        attempts.add(now)
    }

    /**
     * A browser always attaches `Origin` (or at least `Referer`) to a `POST`. When the header is
     * absent the caller is not a browser page and cannot be used as a CSRF vector.
     */
    private fun isSameOriginRequest(headers: Map<String, String>): Boolean {
        val origin = headers["origin"] ?: headers["referer"] ?: return true
        return isSameOrigin(origin, headers["host"])
    }

    private fun isSameOrigin(origin: String, host: String?): Boolean {
        if (host == null) return false
        return try {
            val originHost = java.net.URI(origin).host ?: return false
            originHost.equals(host.substringBefore(':'), true)
        } catch (_: Exception) {
            false
        }
    }

    /** Only installable container types are accepted; everything else is refused outright. */
    private fun sanitizeFileName(name: String?): String? {
        val candidate = name?.substringAfterLast('/')?.substringAfterLast('\\') ?: return null
        val cleaned = candidate.filter { it.isLetterOrDigit() || it in "._-" }
        if (cleaned.isBlank() || cleaned.length > 120 || cleaned.startsWith(".")) return null
        val extension = cleaned.substringAfterLast('.', "").lowercase()
        if (extension !in ALLOWED_EXTENSIONS) return null
        return cleaned
    }

    private fun jsonEscape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", " ")
        .replace("\r", " ")

    private companion object {
        const val CSRF_HEADER = "x-csrf-token"
        const val MAX_LOGIN_ATTEMPTS = 5
        const val MAX_RATE_LIMIT_KEYS = 128
        val ALLOWED_EXTENSIONS = setOf("apk", "apks", "apkm", "xapk", "zip", "obb")
    }
}
