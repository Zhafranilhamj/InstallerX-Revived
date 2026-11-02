// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.network

import com.rosan.installer.domain.settings.model.config.NetworkSourceMode
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode

enum class VirusTotalKeyValidationStatus {
    IDLE,
    VALIDATING,
    VALID,
    INVALID_FORMAT,
    INVALID_KEY,
    NETWORK_ERROR,
    QUOTA_EXCEEDED,
    UNKNOWN,
}

data class VirusTotalKeyValidation(
    val status: VirusTotalKeyValidationStatus = VirusTotalKeyValidationStatus.IDLE,
    val message: String? = null,
)

data class NetworkSettingsState(
    val allowInternetAccess: Boolean = true,
    val networkSourceMode: NetworkSourceMode = NetworkSourceMode.Cache,
    val networkSourceModeWarningAcknowledged: Boolean = false,
    val httpProfile: HttpProfile = HttpProfile.ALLOW_SECURE,
    val githubUpdateChannel: GithubUpdateChannel = GithubUpdateChannel.OFFICIAL,
    val customGithubProxyUrl: String = "",
    val virusTotalApiKey: String = "",
    val virusTotalScanMode: VirusTotalScanMode = VirusTotalScanMode.OFF,
    val virusTotalKeyValidation: VirusTotalKeyValidation = VirusTotalKeyValidation(),
)
