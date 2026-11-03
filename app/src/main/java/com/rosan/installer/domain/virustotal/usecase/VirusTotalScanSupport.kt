// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.usecase

import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.virustotal.model.VirusTotalApiKeyFormat
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalScanError
import kotlinx.coroutines.flow.first

internal data class VirusTotalPreflight(
    val apiKey: String,
    val networkEnabled: Boolean,
    val mode: VirusTotalScanMode,
)

internal suspend fun AppSettingsRepository.loadVirusTotalPreflight(): VirusTotalPreflight {
    val prefs = preferencesFlow.first()
    return VirusTotalPreflight(
        apiKey = prefs.virusTotalApiKey.trim(),
        networkEnabled = prefs.allowInternetAccess,
        mode = prefs.virusTotalScanMode,
    )
}

/** Enforce the network boundary at the use case, not just by disabling visible buttons. */
internal fun VirusTotalPreflight.blockingError(): VirusTotalScanError? = when {
    !networkEnabled || mode == VirusTotalScanMode.OFF -> VirusTotalScanError(
        kind = VirusTotalErrorKind.SCAN_DISABLED,
        message = "VirusTotal scanning or network access is disabled in settings.",
    )

    apiKey.isEmpty() -> VirusTotalScanError(
        kind = VirusTotalErrorKind.NO_API_KEY,
        message = "No VirusTotal API key is configured.",
    )

    !VirusTotalApiKeyFormat.isValid(apiKey) -> VirusTotalScanError(
        kind = VirusTotalErrorKind.INVALID_API_KEY,
        message = "A VirusTotal API key must be 64 hexadecimal characters.",
    )

    else -> null
}
