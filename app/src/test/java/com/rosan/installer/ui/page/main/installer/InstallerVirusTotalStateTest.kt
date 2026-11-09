// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.packageinfo.PackageIdentityStatus
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.usecase.GetAppIconColorUseCase
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.engine.usecase.GetAppLabelUseCase
import com.rosan.installer.domain.privileged.usecase.GetAvailableUsersUseCase
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.VirusTotalScanner
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityScores
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityVerdict
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import com.rosan.installer.domain.virustotal.testAppPreferences
import com.rosan.installer.domain.virustotal.usecase.GetVirusTotalCommunityScoresUseCase
import com.rosan.installer.domain.virustotal.usecase.ScanFileByHashUseCase
import com.rosan.installer.domain.virustotal.usecase.UploadFileForScanUseCase
import com.rosan.installer.domain.virustotal.usecase.VoteOnVirusTotalFileUseCase
import com.rosan.installer.domain.virustotal.virusTotalSettings
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class InstallerVirusTotalStateTest {
    @Test
    fun `successful votes refresh authoritative totals and network changes invalidate the report`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val harness = Harness()
        try {
            harness.load()
            harness.vm.dispatch(InstallerViewAction.ScanWithVirusTotal(harness.data, VirusTotalScanMethod.UPLOAD))
            awaitUntil { harness.vm.uiState.value.virusTotalScan.status == VirusTotalScanStatus.COMPLETED }
            awaitUntil { !harness.vm.uiState.value.virusTotalScan.isCommunityLoading }
            assertEquals(1L, harness.vm.uiState.value.virusTotalScan.report?.communityScores?.harmless?.toLong())
            harness.vm.dispatch(InstallerViewAction.VoteOnVirusTotal(HASH, VirusTotalCommunityVerdict.HARMLESS))
            awaitUntil { harness.vm.uiState.value.virusTotalScan.submittedVote == VirusTotalCommunityVerdict.HARMLESS }
            awaitUntil { harness.vm.uiState.value.virusTotalScan.report?.communityScores?.harmless == 2 }
            assertEquals(listOf(VirusTotalCommunityVerdict.HARMLESS), harness.scanner.votes)
            assertFalse(harness.vm.uiState.value.virusTotalScan.isVoteSubmitting)

            harness.preferences.value = harness.preferences.value.copy(allowInternetAccess = false)
            awaitUntil { harness.vm.uiState.value.virusTotalScan.report == null }
            assertNull(harness.vm.uiState.value.virusTotalScan.submittedVote)
            harness.vm.dispatch(InstallerViewAction.VoteOnVirusTotal(HASH, VirusTotalCommunityVerdict.MALICIOUS))
            assertEquals(1, harness.scanner.votes.size)
        } finally {
            harness.vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `community vote cannot override a strict malicious scan`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val harness = Harness(mode = VirusTotalScanMode.STRICT, verdict = VirusTotalVerdict.MALICIOUS)
        try {
            harness.load()
            awaitUntil { harness.vm.uiState.value.virusTotalScan.status == VirusTotalScanStatus.COMPLETED }
            assertTrue(harness.vm.uiState.value.virusTotalScan.blocksInstallation)
            harness.vm.dispatch(InstallerViewAction.VoteOnVirusTotal(HASH, VirusTotalCommunityVerdict.HARMLESS))
            awaitUntil { harness.vm.uiState.value.virusTotalScan.submittedVote == VirusTotalCommunityVerdict.HARMLESS }
            assertEquals(VirusTotalVerdict.MALICIOUS, harness.vm.uiState.value.virusTotalScan.report?.verdict)
            assertTrue(harness.vm.uiState.value.virusTotalScan.blocksInstallation)
        } finally {
            harness.vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `a rejected vote preserves the previous scores and exposes a retryable error`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val harness = Harness(voteFailure = VirusTotalException(VirusTotalErrorKind.VOTE_FAILED, "denied"))
        try {
            harness.load()
            harness.vm.dispatch(InstallerViewAction.ScanWithVirusTotal(harness.data, VirusTotalScanMethod.UPLOAD))
            awaitUntil { harness.vm.uiState.value.virusTotalScan.status == VirusTotalScanStatus.COMPLETED }
            awaitUntil { !harness.vm.uiState.value.virusTotalScan.isCommunityLoading }
            harness.vm.dispatch(InstallerViewAction.VoteOnVirusTotal(HASH, VirusTotalCommunityVerdict.MALICIOUS))
            awaitUntil { harness.vm.uiState.value.virusTotalScan.communityError != null }
            val scan = harness.vm.uiState.value.virusTotalScan
            assertEquals(VirusTotalErrorKind.VOTE_FAILED, scan.communityError?.kind)
            assertEquals(1, scan.report?.communityScores?.harmless)
            assertNull(scan.submittedVote)
            assertFalse(scan.isVoteSubmitting)
        } finally {
            harness.vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `strict upload retry performs a new scan and honors a malicious result`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val harness = Harness(mode = VirusTotalScanMode.STRICT, verdict = VirusTotalVerdict.MALICIOUS, failFirstUpload = true)
        try {
            harness.load()
            awaitUntil { harness.vm.uiState.value.virusTotalScan.canRetryUpload }
            assertEquals(1, harness.scanner.uploads.get())
            harness.vm.dispatch(InstallerViewAction.RetryVirusTotalUpload)
            awaitUntil { harness.vm.uiState.value.virusTotalScan.status == VirusTotalScanStatus.COMPLETED }
            assertEquals(2, harness.scanner.uploads.get())
            assertTrue(harness.vm.uiState.value.virusTotalScan.blocksInstallation)
            harness.vm.dispatch(InstallerViewAction.RetryVirusTotalUpload)
            assertEquals(2, harness.scanner.uploads.get())
        } finally {
            harness.vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private suspend fun awaitUntil(predicate: () -> Boolean) {
        withTimeout(5_000) {
            while (!predicate()) delay(10)
        }
    }

    private inner class Harness(
        mode: VirusTotalScanMode = VirusTotalScanMode.NORMAL,
        verdict: VirusTotalVerdict = VirusTotalVerdict.CLEAN,
        voteFailure: Exception? = null,
        failFirstUpload: Boolean = false,
    ) {
        val preferences = MutableStateFlow(testAppPreferences(mode, KEY, true))
        val scanner = FakeScanner(verdict, voteFailure, failFirstUpload)
        val session = InstallerSessionRepositoryImpl("virustotal-state-test") {}
        val data = DataEntity.FileEntity("sample.apk")
        private val settings = virusTotalSettings(preferences)
        val vm = InstallerViewModel(
            session = session,
            appSettingsRepo = settings,
            getAvailableUsers = GetAvailableUsersUseCase(stub()),
            getAppIcon = GetAppIconUseCase(stub()),
            getAppIconColor = GetAppIconColorUseCase(stub()),
            getAppLabel = GetAppLabelUseCase(stub { null }),
            deviceCapabilityProvider = stub { true },
            scanFileByHash = lazy { ScanFileByHashUseCase(scanner, settings) },
            uploadFileForScan = lazy { UploadFileForScanUseCase(scanner, settings) },
            getCommunityScores = lazy { GetVirusTotalCommunityScoresUseCase(scanner, settings) },
            voteOnFile = lazy { VoteOnVirusTotalFileUseCase(scanner, settings) },
        )

        fun load() {
            val app = AppEntity.BaseEntity(
                packageName = "example.app",
                sharedUserId = null,
                data = data,
                versionCode = 1,
                versionName = "1",
                label = null,
                icon = null,
                targetSdk = null,
                minSdk = null,
                size = 10,
            )
            session.analysisResults = listOf(
                PackageAnalysisResult(
                    packageName = app.packageName,
                    sessionMode = SessionMode.Single,
                    appEntities = listOf(SelectInstallEntity(app, true)),
                    installedAppInfo = null,
                    signatureCheckPerformed = false,
                    signatureMatchStatus = SignatureMatchStatus.NOT_INSTALLED,
                    identityStatus = PackageIdentityStatus.NOT_APPLICABLE,
                ),
            )
            session.progress.tryEmit(ProgressEntity.InstallAnalysedSuccess)
            vm.dispatch(InstallerViewAction.CollectSession(session))
        }
    }

    private class FakeScanner(private val verdict: VirusTotalVerdict, private val voteFailure: Exception?, private val failFirstUpload: Boolean) : VirusTotalScanner {
        val uploads = AtomicInteger()
        val votes = mutableListOf<VirusTotalCommunityVerdict>()
        private var scores = VirusTotalCommunityScores(1, 0, 1)
        override suspend fun validateApiKey(apiKey: String): Result<Unit> = Result.success(Unit)
        override suspend fun lookupByHash(apiKey: String, sha256: String): VirusTotalReport = report(VirusTotalScanMethod.HASH)
        override suspend fun uploadAndScan(apiKey: String, data: DataEntity, fileName: String): VirusTotalReport {
            if (uploads.incrementAndGet() == 1 && failFirstUpload) {
                throw VirusTotalException(VirusTotalErrorKind.NETWORK_ERROR, "offline")
            }
            return report(VirusTotalScanMethod.UPLOAD)
        }
        override suspend fun getCommunityScores(apiKey: String, sha256: String): VirusTotalCommunityScores = scores
        override suspend fun voteOnFile(apiKey: String, sha256: String, verdict: VirusTotalCommunityVerdict) {
            voteFailure?.let { throw it }
            votes += verdict
            scores = if (verdict == VirusTotalCommunityVerdict.HARMLESS) scores.copy(harmless = scores.harmless + 1) else scores.copy(malicious = scores.malicious + 1)
        }
        private fun report(method: VirusTotalScanMethod) = VirusTotalReport(verdict = verdict, sha256 = HASH, method = method)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> stub(crossinline answer: () -> Any? = { error("Unexpected dependency call") }): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> answer() } as T

    private companion object {
        const val KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        const val HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
