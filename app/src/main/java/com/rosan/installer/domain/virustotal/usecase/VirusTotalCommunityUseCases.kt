// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.usecase

import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.virustotal.VirusTotalScanner
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityScores
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityVerdict
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetVirusTotalCommunityScoresUseCase(
    private val scanner: VirusTotalScanner,
    private val appSettingsRepo: AppSettingsRepository,
) {
    suspend operator fun invoke(sha256: String): Result<VirusTotalCommunityScores> =
        virusTotalCommunityRequest(appSettingsRepo) { apiKey -> scanner.getCommunityScores(apiKey, sha256) }
}

class VoteOnVirusTotalFileUseCase(
    private val scanner: VirusTotalScanner,
    private val appSettingsRepo: AppSettingsRepository,
) {
    suspend operator fun invoke(sha256: String, verdict: VirusTotalCommunityVerdict): Result<Unit> =
        virusTotalCommunityRequest(appSettingsRepo) { apiKey -> scanner.voteOnFile(apiKey, sha256, verdict) }
}

/** Both community operations obey the same network, mode, and API-key checks as scans. */
private suspend fun <T> virusTotalCommunityRequest(
    appSettingsRepo: AppSettingsRepository,
    request: suspend (String) -> T,
): Result<T> = withContext(Dispatchers.IO) {
    try {
        val preflight = appSettingsRepo.loadVirusTotalPreflight()
        preflight.blockingError()?.let {
            return@withContext Result.failure(VirusTotalException(it.kind, it.message))
        }
        Result.success(request(preflight.apiKey))
    } catch (error: CancellationException) {
        throw error
    } catch (error: VirusTotalException) {
        Result.failure(error)
    } catch (error: Exception) {
        Result.failure(VirusTotalException(VirusTotalErrorKind.NETWORK_ERROR, "Could not reach VirusTotal."))
    }
}
