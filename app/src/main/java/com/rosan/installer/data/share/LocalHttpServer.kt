// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import com.rosan.installer.domain.share.model.LocalShareProtocol
import java.io.BufferedInputStream
import java.io.File
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

/**
 * Minimal HTTP/1.1 server used only on the local network. It deliberately avoids a server
 * library dependency: a dashboard page, a PIN login, a status endpoint and a streaming multipart
 * upload are all that is required.
 */
class LocalHttpServer(
    private val port: Int,
    private val pin: String,
    private val receiveDirectory: File,
    private val versionName: String,
    private val onFileReceived: (File) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        receiveDirectory.mkdirs()
        serverSocket = ServerSocket(port, 32, InetAddress.getByName("0.0.0.0"))
        executor.execute { acceptLoop() }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
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
                dispatch(method, path, query, headers, input, output)
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
    ) {
        val cookieToken = headers["cookie"]
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("${LocalShareProtocol.PIN_COOKIE}=") }
            ?.substringAfter('=')
        val suppliedPin = queryPin(query) ?: headers[LocalShareProtocol.PIN_HEADER.lowercase()]
        val authorized = LocalShareProtocol.isAuthorized(pin, cookieToken, suppliedPin)

        when {
            method == "POST" && path == "/login" -> {
                val body = readBody(input, headers)
                val enteredPin = body.substringAfter("pin=", "").substringBefore('&')
                if (LocalShareProtocol.isAuthorized(pin, null, enteredPin)) {
                    respond(
                        output,
                        303,
                        "See Other",
                        "text/plain",
                        "ok".toByteArray(),
                        "Set-Cookie: ${LocalShareProtocol.PIN_COOKIE}=${LocalShareProtocol.pinToken(pin)}; Path=/; HttpOnly; SameSite=Lax\r\nLocation: /\r\n",
                    )
                } else {
                    respondHtml(output, 401, "Unauthorized", LocalShareDashboard.loginPage(error = true))
                }
            }

            !authorized -> respondHtml(output, 401, "Unauthorized", LocalShareDashboard.loginPage())

            method == "GET" && (path == "/" || path == "/index.html") ->
                respondHtml(output, 200, "OK", LocalShareDashboard.mainPage(pin.isNotEmpty(), versionName))

            method == "GET" && path == "/api/status" -> {
                val files = receiveDirectory.listFiles()?.count { it.isFile } ?: 0
                respondJson(output, "{\"running\":true,\"files\":$files,\"pin\":${pin.isNotEmpty()}}")
            }

            method == "GET" && path == "/api/files" -> {
                val names = receiveDirectory.listFiles()
                    ?.filter { it.isFile }
                    ?.sortedByDescending { it.lastModified() }
                    ?.take(50)
                    ?.joinToString(",") { "\"${jsonEscape(it.name)}\"" }
                    .orEmpty()
                respondJson(output, "{\"files\":[$names]}")
            }

            method == "POST" && (path == LocalShareProtocol.UPLOAD_PATH || path == "/push") -> {
                val result = receiveUpload(input, headers)
                if (result == null) {
                    respondJson(output, "{\"ok\":false,\"error\":\"No file received\"}", 400, "Bad Request")
                } else {
                    onFileReceived(result)
                    respondJson(output, "{\"ok\":true,\"name\":\"${jsonEscape(result.name)}\"}")
                }
            }

            else -> respondHtml(output, 404, "Not Found", "<h1>404</h1>")
        }
    }

    private fun receiveUpload(input: BufferedInputStream, headers: Map<String, String>): File? {
        val contentLength = headers["content-length"]?.toLongOrNull() ?: -1L
        if (contentLength > LocalShareProtocol.MAX_UPLOAD_BYTES) return null
        val contentType = headers["content-type"].orEmpty()
        val boundary = Regex("""boundary="?([^";]+)"?""", RegexOption.IGNORE_CASE)
            .find(contentType)?.groupValues?.get(1) ?: return null

        var fileName: String? = null
        var headerLines = 0
        while (headerLines < 40) {
            headerLines++
            val line = readAsciiLine(input) ?: return null
            if (line.contains("filename=", ignoreCase = true)) {
                fileName = Regex("""filename="?([^";]*)"?""", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.get(1)
            }
            if (line.isEmpty()) break
        }
        val safeName = sanitizeFileName(fileName) ?: return null
        val marker = "\r\n--$boundary".toByteArray(Charsets.US_ASCII)
        val target = File(receiveDirectory, safeName)
        val pending = ByteArray(marker.size)
        var pendingLength = 0
        var written = 0L
        var matched = false
        target.outputStream().use { output ->
            while (!matched) {
                val value = input.read()
                if (value < 0) break
                if (written + pendingLength > LocalShareProtocol.MAX_UPLOAD_BYTES) break
                val byte = value.toByte()
                if (byte == marker[pendingLength]) {
                    pending[pendingLength++] = byte
                    if (pendingLength == marker.size) matched = true
                } else {
                    if (pendingLength > 0) {
                        output.write(pending, 0, pendingLength)
                        written += pendingLength
                        pendingLength = 0
                    }
                    if (byte == marker[0]) {
                        pending[pendingLength++] = byte
                    } else {
                        output.write(value)
                        written++
                    }
                }
            }
            if (!matched && pendingLength > 0) {
                output.write(pending, 0, pendingLength)
                written += pendingLength
            }
        }
        if (written <= 0L) {
            target.delete()
            return null
        }
        return target
    }

    private fun queryPin(query: String): String? = query.split('&')
        .firstOrNull { it.substringBefore('=') == "pin" }
        ?.substringAfter('=')

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
        val bytes = mutableListOf<Byte>()
        while (true) {
            val value = input.read()
            if (value < 0) return if (bytes.isEmpty()) null else String(bytes.toByteArray(), Charsets.ISO_8859_1)
            if (value == '\n'.code) {
                if (bytes.lastOrNull() == '\r'.code.toByte()) bytes.removeAt(bytes.lastIndex)
                return String(bytes.toByteArray(), Charsets.ISO_8859_1)
            }
            bytes.add(value.toByte())
            if (bytes.size > 16 * 1024) return null
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

    private fun sanitizeFileName(name: String?): String? {
        val candidate = name?.substringAfterLast('/')?.substringAfterLast('\\') ?: return null
        val cleaned = candidate.filter { it.isLetterOrDigit() || it in "._-" }
        if (cleaned.isBlank() || cleaned.length > 120 || cleaned.startsWith(".")) return null
        return cleaned
    }

    private fun jsonEscape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", " ")
        .replace("\r", " ")
}
