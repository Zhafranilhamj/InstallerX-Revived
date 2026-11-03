// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.model

/** VirusTotal's upload limits, expressed in the same binary MB units as the existing UI. */
object VirusTotalUploadPolicy {
    const val DIRECT_MAX_BYTES = 32L * 1024L * 1024L
    const val MAX_UPLOAD_BYTES = 650L * 1024L * 1024L

    /** A fresh, single-use upload URL is mandatory above the standard endpoint's limit. */
    fun requiresUploadUrl(size: Long): Boolean = size in (DIRECT_MAX_BYTES + 1)..MAX_UPLOAD_BYTES

    fun validationError(size: Long): VirusTotalErrorKind? = when {
        size < 0 -> VirusTotalErrorKind.FILE_UNREADABLE
        size > MAX_UPLOAD_BYTES -> VirusTotalErrorKind.FILE_TOO_LARGE
        else -> null
    }
}
