// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.tools

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.downloader.model.DownloadStatus
import com.rosan.installer.domain.downloader.repository.DownloadRepository
import com.rosan.installer.ui.activity.InstallerActivity
import com.rosan.installer.ui.util.formatSize
import java.io.File
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun RemoteDownloadPage() {
    val context = LocalContext.current
    val repository: DownloadRepository = koinInject()
    val scope = rememberCoroutineScope()
    val history by repository.history.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var errorText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { repository.refresh() }

    ToolsScaffold(title = stringResource(R.string.remote_download_title)) {
        Text(stringResource(R.string.remote_download_intro), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.remote_download_url)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = progress == null,
            onClick = {
                scope.launch {
                    errorText = ""
                    progress = 0f
                    val result = repository.download(url.trim()) { fraction -> progress = fraction }
                    progress = null
                    val record = result.getOrNull()
                    if (record != null && record.status == DownloadStatus.COMPLETED) {
                        openPackage(context, File(record.localPath))
                    } else {
                        errorText = record?.error ?: result.exceptionOrNull()?.message.orEmpty()
                    }
                }
            },
        ) { Text(stringResource(R.string.remote_download_start)) }
        progress?.let { fraction ->
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        }
        if (errorText.isNotBlank()) {
            Text(errorText, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.remote_download_history), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { scope.launch { repository.clear() } }) { Text(stringResource(R.string.remote_download_clear)) }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(history, key = { it.id }) { record ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(record.fileName, style = MaterialTheme.typography.titleSmall)
                        Text(record.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Text(
                            "${record.size.formatSize()} · ${record.status.name}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (record.status == DownloadStatus.COMPLETED) {
                                TextButton(onClick = { openPackage(context, File(record.localPath)) }) {
                                    Text(stringResource(R.string.remote_download_install))
                                }
                            }
                            TextButton(onClick = { scope.launch { repository.remove(record.id) } }) {
                                Text(stringResource(R.string.remote_download_remove))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openPackage(context: android.content.Context, file: File) {
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return
    val intent = Intent(context, InstallerActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "package", uri)
    }
    runCatching { context.startActivity(intent) }
}
