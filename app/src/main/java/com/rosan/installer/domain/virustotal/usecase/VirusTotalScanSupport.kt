// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.usecase

import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalScanError
import kotlinx.coroutines.flow.first

internal data class VirusTotalPreflight(
    val apiKey: String,
    val networkEnabled: Boolean,
)

internal suspend fun AppSettingsRepository.loadVirusTotalPreflight(): VirusTotalPreflight {
    val prefs = preferencesFlow.first()
    return VirusTotalPreflight(
        apiKey = prefs.virusTotalApiKey.trim(),
        networkEnabled = prefs.allowInternetAccess,
    )
}

/**
 * Returns the reason a scan cannot run, or null when the preflight checks pass.
 */
internal fun VirusTotalPreflight.blockingError(): VirusTotalScanError? = when {
    !networkEnabled -> VirusTotalScanError(
        kind = VirusTotalErrorKind.NETWORK_ERROR,
        message = "Network access is disabled in Network settings.",
    )

    apiKey.isEmpty() -> VirusTotalScanError(
        kind = VirusTotalErrorKind.NO_API_KEY,
        message = "No VirusTotal API key is configured.",
    )

    else -> null
}
