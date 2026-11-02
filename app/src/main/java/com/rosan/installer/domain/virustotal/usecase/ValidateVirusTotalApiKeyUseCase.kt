// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.usecase

import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.virustotal.VirusTotalScanner
import com.rosan.installer.domain.virustotal.model.VirusTotalApiKeyFormat
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Validates a VirusTotal API key. The key format is checked locally first so obviously malformed
 * input never reaches the API.
 */
class ValidateVirusTotalApiKeyUseCase(
    private val scanner: VirusTotalScanner,
    private val appSettingsRepo: AppSettingsRepository,
) {
    suspend operator fun invoke(apiKey: String): Result<Unit> {
        if (!appSettingsRepo.preferencesFlow.first().allowInternetAccess) {
            return Result.failure(
                VirusTotalException(
                    kind = VirusTotalErrorKind.NETWORK_ERROR,
                    message = "Network access is disabled in Network settings.",
                ),
            )
        }

        val trimmed = apiKey.trim()
        if (!VirusTotalApiKeyFormat.isValid(trimmed)) {
            return Result.failure(
                VirusTotalException(
                    kind = VirusTotalErrorKind.INVALID_API_KEY,
                    message = "A VirusTotal API key must be 64 hexadecimal characters.",
                ),
            )
        }

        return try {
            scanner.validateApiKey(trimmed)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(
                VirusTotalException(
                    kind = VirusTotalErrorKind.UNKNOWN,
                    message = "Could not validate the VirusTotal API key.",
                ),
            )
        }
    }
}
