// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.usecase

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.virustotal.VirusTotalScanner
import com.rosan.installer.domain.virustotal.calculateSha256
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.model.VirusTotalScanError
import com.rosan.installer.domain.virustotal.model.VirusTotalScanOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Checks a file against VirusTotal using only its SHA-256 hash.
 */
class ScanFileByHashUseCase(
    private val scanner: VirusTotalScanner,
    private val appSettingsRepo: AppSettingsRepository,
) {
    suspend operator fun invoke(data: DataEntity): VirusTotalScanOutcome = withContext(Dispatchers.IO) {
        val preflight = try {
            appSettingsRepo.loadVirusTotalPreflight()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return@withContext VirusTotalScanOutcome.Failure(
                VirusTotalScanError(
                    kind = VirusTotalErrorKind.UNKNOWN,
                    message = "Could not read the VirusTotal settings.",
                ),
            )
        }
        preflight.blockingError()?.let { error ->
            return@withContext VirusTotalScanOutcome.Failure(error)
        }

        val sha256 = data.calculateSha256()
        if (sha256 == null) {
            return@withContext VirusTotalScanOutcome.Failure(
                VirusTotalScanError(
                    kind = VirusTotalErrorKind.FILE_UNREADABLE,
                    message = "The file could not be read for hashing.",
                ),
            )
        }

        try {
            val report = scanner.lookupByHash(preflight.apiKey, sha256)
            VirusTotalScanOutcome.Success(report)
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            Timber.w(error, "VirusTotal hash lookup failed")
            VirusTotalScanOutcome.Failure(VirusTotalScanError(error.kind, error.message.orEmpty()))
        } catch (error: Exception) {
            Timber.e(error, "Unexpected VirusTotal hash lookup failure")
            VirusTotalScanOutcome.Failure(
                VirusTotalScanError(
                    kind = VirusTotalErrorKind.NETWORK_ERROR,
                    message = error.message ?: "Could not reach VirusTotal.",
                ),
            )
        }
    }
}
