// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.share.model

import java.net.URI
import java.security.MessageDigest

/** Connection details encoded in the QR code shown by the TV-side dashboard. */
data class LocalShareEndpoint(
    val baseUrl: String,
    val pin: String = "",
)

/** Pure protocol helpers for the local HTTP dashboard and the "Send to TV" QR handshake. */
object LocalShareProtocol {
    const val DEFAULT_PORT = 8765
    const val QR_HOST = "tv"
    const val PIN_COOKIE = "ix_session"
    const val PIN_HEADER = "X-InstallerX-Pin"
    const val UPLOAD_PATH = "/upload"
    const val MAX_UPLOAD_BYTES = 1024L * 1024 * 1024

    fun isValidPort(port: Int): Boolean = port in 1024..65535

    /** An empty PIN disables security; otherwise 4-8 digits are required. */
    fun isValidPin(pin: String): Boolean = pin.isEmpty() || (pin.length in 4..8 && pin.all { it in '0'..'9' })

    fun normalizeBaseUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host ?: return null
        val port = if (uri.port == -1) DEFAULT_PORT else uri.port
        if (!isValidPort(port)) return null
        val path = uri.path?.trimEnd('/').orEmpty()
        return "$scheme://$host:$port$path"
    }

    fun buildQrPayload(baseUrl: String, pin: String): String {
        val normalized = normalizeBaseUrl(baseUrl) ?: return ""
        return "installerx://$QR_HOST?u=${percentEncode(normalized)}&p=${percentEncode(pin)}"
    }

    fun parseQrPayload(payload: String): LocalShareEndpoint? {
        val uri = runCatching { URI(payload.trim()) }.getOrNull() ?: return null
        if (!uri.scheme.equals("installerx", ignoreCase = true)) return null
        val host = uri.host?.lowercase() ?: return null
        if (host != QR_HOST && host != "share") return null
        val query = uri.rawQuery.orEmpty()
        val values = query.split('&').associate { part ->
            val (key, value) = part.split('=', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            percentDecode(key) to percentDecode(value)
        }
        val baseUrl = normalizeBaseUrl(values["u"].orEmpty()) ?: return null
        val pin = values["p"].orEmpty()
        if (!isValidPin(pin)) return null
        return LocalShareEndpoint(baseUrl, pin)
    }

    fun pinToken(pin: String): String = MessageDigest.getInstance("SHA-256")
        .digest(pin.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    /** Constant-time comparison of the PIN-derived session token. */
    fun isAuthorized(
        configuredPin: String,
        cookieToken: String?,
        suppliedPin: String?,
    ): Boolean {
        if (configuredPin.isEmpty()) return true
        val expected = pinToken(configuredPin).toByteArray(Charsets.US_ASCII)
        val candidates = listOfNotNull(
            cookieToken?.toByteArray(Charsets.US_ASCII),
            suppliedPin?.takeIf(::isValidPin)?.let { pinToken(it).toByteArray(Charsets.US_ASCII) },
        )
        return candidates.any { MessageDigest.isEqual(expected, it) }
    }

    /** Accepts only RFC1918 / loopback / link-local peers, never a public WAN address. */
    fun isLocalPeer(hostAddress: String): Boolean {
        val address = hostAddress.substringBefore('%')
        if (address == "127.0.0.1" || address == "::1" || address == "0:0:0:0:0:0:0:1") return true
        val parts = address.split('.')
        if (parts.size == 4) {
            val octets = parts.mapNotNull { it.toIntOrNull() }
            if (octets.size != 4) return false
            val (a, b) = octets
            return a == 10 ||
                (a == 172 && b in 16..31) ||
                (a == 192 && b == 168) ||
                (a == 169 && b == 254) ||
                a == 127
        }
        return address.startsWith("fe80:", ignoreCase = true) || address.startsWith("fc", ignoreCase = true)
    }

    fun percentEncode(value: String): String = buildString {
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt() and 0xff
            if ((c in 'a'.code..'z'.code) || (c in 'A'.code..'Z'.code) || (c in '0'.code..'9'.code) || c == '-'.code || c == '.'.code || c == '_'.code || c == '~'.code) {
                append(c.toChar())
            } else {
                append('%')
                append("%02X".format(c))
            }
        }
    }

    private fun percentDecode(value: String): String {
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index < value.length) {
            if (value[index] == '%' && index + 2 < value.length) {
                val code = value.substring(index + 1, index + 3).toIntOrNull(16)
                if (code != null) {
                    bytes.add(code.toByte())
                    index += 3
                    continue
                }
            }
            bytes.add(value[index].code.toByte())
            index++
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }
}
