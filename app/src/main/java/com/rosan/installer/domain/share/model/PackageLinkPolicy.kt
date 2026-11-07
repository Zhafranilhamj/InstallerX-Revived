// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.share.model

import java.net.URI

/**
 * Pure validation for package links handed to [com.rosan.installer.ui.activity.LinkIntakeActivity].
 *
 * Kept free of Android APIs so the rules can be unit tested on the JVM.
 */
object PackageLinkPolicy {
    /** Extensions the installer can actually open. `.zip` covers `.apks.zip` style containers. */
    val ALLOWED_EXTENSIONS: List<String> = listOf(".apk", ".apks", ".apkm", ".xapk", ".zip")

    private val ALLOWED_SCHEMES = setOf("http", "https")

    /**
     * Accepts an absolute http(s) package URL with a supported container extension.
     *
     * Rejected on purpose:
     *  * non-network schemes (`javascript:`, `file:`, `content:`, `data:`, `intent:`),
     *  * URLs carrying credentials (`https://user:pass@host/app.apk`),
     *  * loopback hosts, which are only reachable from the device itself and are a common
     *    SSRF/redirect trick (LAN peers stay allowed because of the "Send to TV" flow).
     */
    fun isAllowedPackageUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme !in ALLOWED_SCHEMES) return false
        if (!uri.userInfo.isNullOrEmpty() || !uri.rawUserInfo.isNullOrEmpty()) return false
        if (uri.host.isNullOrBlank()) return false
        if (isLoopbackHost(uri.host)) return false
        val path = uri.path.orEmpty().lowercase()
        if (path.isEmpty()) return false
        return ALLOWED_EXTENSIONS.any { path.endsWith(it) }
    }

    private fun isLoopbackHost(host: String): Boolean {
        val name = host.lowercase().removePrefix("[").removeSuffix("]")
        if (name == "localhost") return true
        if (name == "::1" || name == "0:0:0:0:0:0:0:1") return true
        val octets = name.split('.')
        if (octets.size != 4) return false
        val numbers = octets.map { it.toIntOrNull() ?: return false }
        return numbers.all { it in 0..255 } && numbers[0] == 127
    }
}
