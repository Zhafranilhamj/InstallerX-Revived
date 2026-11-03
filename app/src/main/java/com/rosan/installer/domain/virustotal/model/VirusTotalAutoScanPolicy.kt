// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.model

import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode

/**
 * Resolves the scan that starts on its own when the install confirmation screen opens.
 *
 * Normal mode performs an advisory SHA-256 hash lookup: no file bytes leave the device and the
 * result never blocks installation. Strict mode keeps its mandatory upload gate. Off mode does
 * not start any request.
 */
fun VirusTotalScanMode.automaticScanMethod(): VirusTotalScanMethod? = when (this) {
    VirusTotalScanMode.OFF -> null
    VirusTotalScanMode.NORMAL -> VirusTotalScanMethod.HASH
    VirusTotalScanMode.STRICT -> VirusTotalScanMethod.UPLOAD
}

/** Only Strict mode is allowed to delay or refuse an install because of a scan. */
fun VirusTotalScanMode.isMandatoryScan(): Boolean = this == VirusTotalScanMode.STRICT
