// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.rosan.installer.R
import com.rosan.installer.data.share.LocalShareClient
import com.rosan.installer.data.share.LocalShareManager
import com.rosan.installer.domain.share.model.LocalShareEndpoint
import com.rosan.installer.domain.share.model.LocalShareProtocol
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import java.io.File
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SendToTvPage() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val manager: LocalShareManager = koinInject()
    val client: LocalShareClient = koinInject()
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var resultText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        manager.pendingEndpoint.collect { endpoint ->
            if (endpoint != null) {
                baseUrl = endpoint.baseUrl
                pin = endpoint.pin
                manager.consumePendingEndpoint()
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/')?.filter { it.isLetterOrDigit() || it in "._-" }
            ?.takeIf { it.isNotBlank() } ?: "package.apk"
        val target = File(context.cacheDir, "tv_push/$name").apply { parentFile?.mkdirs() }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            selectedFile = target
        }.onFailure { resultText = it.message.orEmpty() }
    }

    ToolsScaffold(title = stringResource(R.string.send_to_tv_title)) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.send_to_tv_intro), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { navigator.push(Route.QrScanner) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.send_to_tv_scan_qr))
            }
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text(stringResource(R.string.send_to_tv_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = { Text(stringResource(R.string.local_share_pin)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedFile?.name ?: stringResource(R.string.send_to_tv_pick_file))
            }
            Button(
                onClick = {
                    val endpoint = LocalShareProtocol.normalizeBaseUrl(baseUrl)?.let { LocalShareEndpoint(it, pin) }
                    val file = selectedFile
                    when {
                        endpoint == null -> resultText = context.getString(R.string.send_to_tv_invalid_address)

                        file == null -> resultText = context.getString(R.string.send_to_tv_no_file)

                        else -> scope.launch {
                            sending = true
                            val outcome = client.push(endpoint, file)
                            sending = false
                            resultText = outcome.fold(
                                onSuccess = { context.getString(R.string.send_to_tv_success) },
                                onFailure = { context.getString(R.string.send_to_tv_failed, it.message.orEmpty()) },
                            )
                        }
                    }
                },
                enabled = !sending,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.send_to_tv_send)) }
            if (resultText.isNotBlank()) {
                Text(resultText, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
            }
            Text(stringResource(R.string.send_to_tv_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}
