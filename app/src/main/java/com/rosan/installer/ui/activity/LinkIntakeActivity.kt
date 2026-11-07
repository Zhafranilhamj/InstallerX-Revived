// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.R
import com.rosan.installer.data.share.LocalShareManager
import com.rosan.installer.domain.downloader.model.DownloadStatus
import com.rosan.installer.domain.downloader.repository.DownloadRepository
import com.rosan.installer.domain.share.model.LocalShareProtocol
import com.rosan.installer.domain.share.model.PackageLinkPolicy
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Intake point for package links from browsers, messaging apps and "Send to TV" QR codes.
 *
 * HTTP(S) links are downloaded through the network-gated downloader and then handed to the
 * normal installer activity, so all existing profile/authorizer logic stays in one place.
 */
class LinkIntakeActivity :
    ComponentActivity(),
    KoinComponent {
    private val downloadRepository: DownloadRepository by inject()
    private val localShareManager: LocalShareManager by inject()
    private val status = MutableStateFlow("")
    private val awaitingConfirmation = MutableStateFlow(false)
    private val downloadInFlight = MutableStateFlow(false)
    private var downloadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status.value = getString(R.string.intake_working)
        setContent {
            val message by status.collectAsStateWithLifecycle()
            val awaiting by awaitingConfirmation.collectAsStateWithLifecycle()
            val downloading by downloadInFlight.collectAsStateWithLifecycle()
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    ) {
                        if (awaiting) {
                            Text(text = message.ifBlank { stringResource(R.string.intake_working) })
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            ) {
                                Button(
                                    onClick = { confirmDownload() },
                                    enabled = !downloading,
                                ) { Text(stringResource(R.string.intake_confirm_action)) }
                                OutlinedButton(
                                    onClick = { cancelDownload() },
                                    enabled = !downloading,
                                ) { Text(stringResource(R.string.intake_cancel_action)) }
                            }
                        } else {
                            CircularProgressIndicator()
                            Text(text = message.ifBlank { stringResource(R.string.intake_working) })
                            Button(onClick = { finish() }) { Text(stringResource(R.string.close)) }
                        }
                    }
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // Pending URL awaiting user confirmation
    private var pendingUrl: String? = null

    private fun handleIntent(incoming: Intent?) {
        val data = incoming?.data
        when {
            incoming?.action == Intent.ACTION_SEND -> {
                val text = incoming.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val url = URL_REGEX.find(text)?.value
                if (url == null) fail(R.string.intake_invalid_link) else requestConfirmation(url)
            }

            data?.scheme.equals("installerx", ignoreCase = true) -> handleDeepLink(data.toString())

            data?.scheme?.lowercase().orEmpty() in setOf("http", "https") -> requestConfirmation(data.toString())

            else -> fail(R.string.intake_invalid_link)
        }
    }

    private fun handleDeepLink(payload: String) {
        val host = runCatching { android.net.Uri.parse(payload).host?.lowercase() }.getOrNull()
        when (host) {
            LocalShareProtocol.QR_HOST, "share" -> {
                val endpoint = LocalShareProtocol.parseQrPayload(payload)
                if (endpoint == null) {
                    fail(R.string.intake_invalid_link)
                } else {
                    localShareManager.setPendingEndpoint(endpoint)
                    openSettings(SettingsActivity.ROUTE_SEND_TO_TV)
                }
            }

            "download", "install" -> {
                val url = runCatching { android.net.Uri.parse(payload).getQueryParameter("url") }.getOrNull()
                    ?: runCatching { android.net.Uri.parse(payload).getQueryParameter("u") }.getOrNull()
                if (url.isNullOrBlank()) fail(R.string.intake_invalid_link) else requestConfirmation(url)
            }

            else -> fail(R.string.intake_invalid_link)
        }
    }

    private fun requestConfirmation(url: String) {
        if (!isAllowedUrl(url)) {
            fail(R.string.intake_invalid_link)
            Timber.w("Blocked untrusted intake URL")
            return
        }
        pendingUrl = url
        status.value = getString(R.string.intake_confirm_message, url)
        awaitingConfirmation.value = true
    }

    fun confirmDownload() {
        val url = pendingUrl ?: return
        if (downloadJob?.isActive == true || downloadInFlight.value) return
        awaitingConfirmation.value = false
        startDownload(url)
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloadInFlight.value = false
        pendingUrl = null
        awaitingConfirmation.value = false
        status.value = getString(R.string.intake_cancelled)
    }

    private fun isAllowedUrl(url: String): Boolean = PackageLinkPolicy.isAllowedPackageUrl(url)

    private fun startDownload(url: String) {
        downloadJob?.cancel()
        downloadJob = lifecycleScope.launch {
            downloadInFlight.value = true
            status.value = getString(R.string.intake_downloading)
            try {
                val result = downloadRepository.download(url)
                val record = result.getOrNull()
                if (record != null && record.status == DownloadStatus.COMPLETED && record.localPath.isNotBlank()) {
                    openInstaller(File(record.localPath))
                } else {
                    val reason = record?.error ?: result.exceptionOrNull()?.message.orEmpty()
                    status.value = getString(R.string.intake_download_failed, reason.ifBlank { "unknown error" })
                    Timber.w("Package link intake failed")
                }
            } finally {
                downloadInFlight.value = false
            }
        }
    }

    private fun openInstaller(file: File) {
        val uri = runCatching {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        }.getOrElse {
            fail(R.string.intake_invalid_link)
            return
        }
        val intent = Intent(this, InstallerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(contentResolver, "package", uri)
        }
        runCatching { startActivity(intent) }
        finish()
    }

    private fun openSettings(route: String) {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(SettingsActivity.EXTRA_INITIAL_ROUTE, route)
        }
        startActivity(intent)
        finish()
    }

    private fun fail(messageResId: Int) {
        status.value = getString(messageResId)
    }

    private companion object {
        val URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    }
}
