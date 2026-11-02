// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.model.VirusTotalApiKeyFormat
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanError
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict

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
     * Strict mode treats the VirusTotal upload scan as a gate: installation waits for the scan to
     * finish and is refused while the report is a real malicious verdict.
     */
    val isStrict: Boolean
        get() = mode == VirusTotalScanMode.STRICT

    /** The report is a real threat verdict rather than one of the common false positives. */
    val reportIsMalware: Boolean
        get() = report?.verdict == VirusTotalVerdict.MALICIOUS

    /**
     * Strict mode refuses to install a file that VirusTotal actually detects as malware. Normal
     * mode never blocks installation because scanning there is advisory only.
     */
    val isBlockedByMalware: Boolean
        get() = mode == VirusTotalScanMode.STRICT && reportIsMalware

    /** The report only matched the detection patterns that are routinely false positives. */
    val isLikelyFalsePositive: Boolean
        get() = mode != VirusTotalScanMode.OFF && report?.verdict == VirusTotalVerdict.LIKELY_FALSE_POSITIVE

    /**
     * The install button is disabled while the mandatory scan runs, and while Strict mode has a
     * malicious verdict for the file.
     */
    val blocksInstallation: Boolean
        get() = (mode == VirusTotalScanMode.STRICT && isScanning) || isBlockedByMalware

    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    /** The stored key also matches the format VirusTotal issues, so a scan can be attempted. */
    val hasUsableApiKey: Boolean
        get() = VirusTotalApiKeyFormat.isValid(apiKey)
}
