// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityScores
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityVerdict
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalScanOutcome
import com.rosan.installer.domain.virustotal.model.VirusTotalUploadPolicy
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import com.rosan.installer.domain.virustotal.usecase.GetVirusTotalCommunityScoresUseCase
import com.rosan.installer.domain.virustotal.usecase.ScanFileByHashUseCase
import com.rosan.installer.domain.virustotal.usecase.UploadFileForScanUseCase
import com.rosan.installer.domain.virustotal.usecase.VoteOnVirusTotalFileUseCase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class VirusTotalNetworkBoundaryTest {
    @Test
    fun `disabled networking disabled scanning and malformed keys stop every request`() = runTest {
        val file = File.createTempFile("vt-boundary-", ".apk").apply { writeText("payload") }
        try {
            for (prefs in listOf(
                testAppPreferences(virusTotalScanMode = VirusTotalScanMode.NORMAL, virusTotalApiKey = KEY, allowInternetAccess = false),
                testAppPreferences(virusTotalScanMode = VirusTotalScanMode.OFF, virusTotalApiKey = KEY, allowInternetAccess = true),
                testAppPreferences(virusTotalScanMode = VirusTotalScanMode.NORMAL, virusTotalApiKey = "bad", allowInternetAccess = true),
            )) {
                val scanner = RecordingScanner()
                val settings = virusTotalSettings(prefs)
                val data = DataEntity.FileEntity(file.path)
                assertIs<VirusTotalScanOutcome.Failure>(UploadFileForScanUseCase(scanner, settings)(data))
                assertIs<VirusTotalScanOutcome.Failure>(ScanFileByHashUseCase(scanner, settings)(data))
                assertTrue(VoteOnVirusTotalFileUseCase(scanner, settings)(HASH, VirusTotalCommunityVerdict.HARMLESS).isFailure)
                assertTrue(GetVirusTotalCommunityScoresUseCase(scanner, settings)(HASH).isFailure)
                assertTrue(scanner.calls.isEmpty())
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `enabled requests reach the scanner and oversized uploads stop at preflight`() = runTest {
        val file = File.createTempFile("vt-boundary-", ".apk").apply { writeText("payload") }
        try {
            val scanner = RecordingScanner()
            val settings = virusTotalSettings(testAppPreferences(virusTotalScanMode = VirusTotalScanMode.NORMAL, virusTotalApiKey = KEY, allowInternetAccess = true))
            val data = DataEntity.FileEntity(file.path)
            assertIs<VirusTotalScanOutcome.Success>(UploadFileForScanUseCase(scanner, settings)(data))
            assertIs<VirusTotalScanOutcome.Success>(ScanFileByHashUseCase(scanner, settings)(data))
            assertTrue(VoteOnVirusTotalFileUseCase(scanner, settings)(HASH, VirusTotalCommunityVerdict.MALICIOUS).isSuccess)
            assertEquals(listOf("upload", "hash", "vote:malicious"), scanner.calls)
            val oversized = object : DataEntity.FileEntity(file.path) {
                override fun getSize(): Long = VirusTotalUploadPolicy.MAX_UPLOAD_BYTES + 1
            }
            val failure = assertIs<VirusTotalScanOutcome.Failure>(UploadFileForScanUseCase(scanner, settings)(oversized))
            assertEquals(VirusTotalErrorKind.FILE_TOO_LARGE, failure.error.kind)
            assertEquals(3, scanner.calls.size)
        } finally {
            file.delete()
        }
    }

    private class RecordingScanner : VirusTotalScanner {
        val calls = mutableListOf<String>()
        private val report = VirusTotalReport(verdict = VirusTotalVerdict.CLEAN, sha256 = HASH, method = VirusTotalScanMethod.HASH)
        override suspend fun validateApiKey(apiKey: String): Result<Unit> = Result.success(Unit)
        override suspend fun lookupByHash(apiKey: String, sha256: String): VirusTotalReport {
            calls += "hash"
            return report
        }
        override suspend fun uploadAndScan(apiKey: String, data: DataEntity, fileName: String): VirusTotalReport {
            calls += "upload"
            return report
        }
        override suspend fun getCommunityScores(apiKey: String, sha256: String): VirusTotalCommunityScores {
            calls += "scores"
            return VirusTotalCommunityScores(1, 0, 1)
        }
        override suspend fun voteOnFile(apiKey: String, sha256: String, verdict: VirusTotalCommunityVerdict) {
            calls += "vote:${verdict.apiValue}"
        }
    }

    private companion object {
        const val KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        const val HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
