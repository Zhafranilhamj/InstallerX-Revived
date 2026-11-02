// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.usecase

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.virustotal.VirusTotalScanner
import com.rosan.installer.domain.virustotal.displayName
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.model.VirusTotalScanError
import com.rosan.installer.domain.virustotal.model.VirusTotalScanOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Uploads a file to VirusTotal and waits for the analysis to finish.
 *
 * The public VirusTotal API accepts direct uploads up to 32 MB. Larger files are rejected with a
 * typed error so the UI can explain the limitation and let the user decide how to proceed.
 */
class UploadFileForScanUseCase(
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

        val size = data.getSize()
        if (size > MAX_UPLOAD_BYTES) {
            return@withContext VirusTotalScanOutcome.Failure(
                VirusTotalScanError(
                    kind = VirusTotalErrorKind.FILE_TOO_LARGE,
                    message = "The file is larger than the 32 MB upload limit.",
                ),
            )
        }

        try {
            val report = scanner.uploadAndScan(
                apiKey = preflight.apiKey,
                data = data,
                fileName = data.displayName(),
            )
            VirusTotalScanOutcome.Success(report)
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            Timber.w(error, "VirusTotal upload failed")
            VirusTotalScanOutcome.Failure(VirusTotalScanError(error.kind, error.message.orEmpty()))
        } catch (error: Exception) {
            Timber.e(error, "Unexpected VirusTotal upload failure")
            VirusTotalScanOutcome.Failure(
                VirusTotalScanError(
                    kind = VirusTotalErrorKind.NETWORK_ERROR,
                    message = error.message ?: "Could not reach VirusTotal.",
                ),
            )
        }
    }

    private companion object {
        const val MAX_UPLOAD_BYTES = 32L * 1024L * 1024L
    }
}
