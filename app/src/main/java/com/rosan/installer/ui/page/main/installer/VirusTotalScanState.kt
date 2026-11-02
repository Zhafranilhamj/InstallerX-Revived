// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanError
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod

enum class VirusTotalScanStatus {
    IDLE,
    SCANNING,
    COMPLETED,
    FAILED,
}

/**
 * UI state for the VirusTotal card shown on the install confirmation screen.
 *
 * The mode, key, and network flag are mirrored from preferences so the card can render the
 * "no API key" hint without an extra settings read.
 */
data class VirusTotalScanState(
    val mode: VirusTotalScanMode = VirusTotalScanMode.OFF,
    val apiKey: String = "",
    val networkEnabled: Boolean = true,
    val status: VirusTotalScanStatus = VirusTotalScanStatus.IDLE,
    val method: VirusTotalScanMethod? = null,
    val report: VirusTotalReport? = null,
    val error: VirusTotalScanError? = null,
    val packageName: String? = null,
    val mandatory: Boolean = false,
) {
    val isScanning: Boolean
        get() = status == VirusTotalScanStatus.SCANNING

    /**
     * Strict mode blocks the install button while the mandatory upload scan is running.
     */
    val blocksInstallation: Boolean
        get() = mode == VirusTotalScanMode.STRICT && isScanning

    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}
