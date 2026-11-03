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
import com.rosan.installer.domain.virustotal.model.VirusTotalUploadPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Streams files up to 650 MB, using VirusTotal's special upload URL above 32 MB. */
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
                VirusTotalScanError(VirusTotalErrorKind.UNKNOWN, "Could not read the VirusTotal settings."),
            )
        }
        preflight.blockingError()?.let { return@withContext VirusTotalScanOutcome.Failure(it) }

        val size = data.getSize()
        VirusTotalUploadPolicy.validationError(size)?.let { kind ->
            return@withContext VirusTotalScanOutcome.Failure(
                VirusTotalScanError(
                    kind = kind,
                    message = when (kind) {
                        VirusTotalErrorKind.FILE_TOO_LARGE -> "The file is larger than the 650 MB upload limit."
                        else -> "The file size could not be determined for upload."
                    },
                ),
            )
        }

        try {
            VirusTotalScanOutcome.Success(
                scanner.uploadAndScan(apiKey = preflight.apiKey, data = data, fileName = data.displayName()),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            Timber.w(error, "VirusTotal upload failed")
            VirusTotalScanOutcome.Failure(VirusTotalScanError(error.kind, error.message.orEmpty()))
        } catch (error: Exception) {
            Timber.e(error, "Unexpected VirusTotal upload failure")
            VirusTotalScanOutcome.Failure(
                VirusTotalScanError(VirusTotalErrorKind.NETWORK_ERROR, error.message ?: "Could not reach VirusTotal."),
            )
        }
    }
}
