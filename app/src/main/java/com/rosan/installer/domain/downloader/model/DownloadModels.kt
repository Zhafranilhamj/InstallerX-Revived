// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.downloader.model

import java.net.URI
import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus { RUNNING, COMPLETED, FAILED }

@Serializable
data class DownloadRecord(
    val id: String,
    val url: String,
    val fileName: String,
    val size: Long = 0,
    val status: DownloadStatus = DownloadStatus.COMPLETED,
    val timestamp: Long = 0,
    val localPath: String = "",
    val error: String? = null,
)

/** Pure URL rules for the remote downloader and deep-link intake. */
object DownloadUrlPolicy {
    val PACKAGE_EXTENSIONS = setOf("apk", "apks", "apkm", "xapk", "zip", "obb")

    fun isSupported(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "http" || scheme == "https") &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }

    fun looksLikePackage(url: String): Boolean {
        val path = runCatching { URI(url.trim()).path }.getOrNull() ?: return false
        return PACKAGE_EXTENSIONS.any { path.endsWith(".$it", ignoreCase = true) }
    }

    fun suggestedFileName(url: String): String {
        val path = runCatching { URI(url.trim()).path }.getOrNull().orEmpty()
        val candidate = path.substringAfterLast('/').substringBefore('?')
            .filter { it.isLetterOrDigit() || it in "._-" }
        return candidate.takeIf { it.length in 2..120 && it.contains('.') } ?: "downloaded.apk"
    }
}
