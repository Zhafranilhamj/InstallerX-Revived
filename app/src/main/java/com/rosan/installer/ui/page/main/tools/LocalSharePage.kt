// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.tools

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.data.obb.ObbSafTreeStore
import com.rosan.installer.data.share.LocalShareManager
import com.rosan.installer.data.share.QrCodeGenerator
import com.rosan.installer.domain.share.model.LocalShareConfig
import com.rosan.installer.domain.share.model.LocalShareProtocol
import com.rosan.installer.framework.service.LocalShareService
import org.koin.compose.koinInject

@Composable
fun LocalSharePage() {
    val context = LocalContext.current
    val manager: LocalShareManager = koinInject()
    val safTreeStore: ObbSafTreeStore = koinInject()
    val state by manager.state.collectAsStateWithLifecycle()
    var port by remember(state.config.port) { mutableStateOf(state.config.port.toString()) }
    var pin by remember(state.config.pin) { mutableStateOf(state.config.pin) }
    var safGranted by remember { mutableStateOf(safTreeStore.get() != null) }

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            safTreeStore.set(uri)
            safGranted = true
        }
    }

    val qrPayload = state.primaryUrl?.let { LocalShareProtocol.buildQrPayload(it, state.config.pin) }
    val qrBitmap = remember(qrPayload) { qrPayload?.let(QrCodeGenerator::encode) }

    ToolsScaffold(title = stringResource(R.string.local_share_title)) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.local_share_intro), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.local_share_port)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = { Text(stringResource(R.string.local_share_pin)) },
                supportingText = { Text(stringResource(R.string.local_share_pin_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.local_share_secure), modifier = Modifier.weight(1f))
                Switch(checked = pin.isNotEmpty(), onCheckedChange = { enabled -> pin = if (enabled) generatePin() else "" })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val config = LocalShareConfig(port.toIntOrNull() ?: LocalShareProtocol.DEFAULT_PORT, pin)
                    if (config.valid) LocalShareService.start(context, config)
                }) { Text(stringResource(R.string.local_share_start)) }
                OutlinedButton(onClick = { LocalShareService.stop(context) }) { Text(stringResource(R.string.local_share_stop)) }
            }
            Text(
                text = if (state.running) stringResource(R.string.local_share_running) else stringResource(R.string.local_share_stopped),
                color = if (state.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.addresses.forEach { address ->
                Text("http://$address:${state.config.port}", style = MaterialTheme.typography.bodySmall)
            }
            state.lastError?.let { error -> Text(error, color = MaterialTheme.colorScheme.error) }
            qrBitmap?.let { bitmap ->
                Card(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.local_share_qr),
                        modifier = Modifier.size(220.dp),
                    )
                }
                Text(stringResource(R.string.local_share_qr_hint), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { treeLauncher.launch(null) }) {
                Text(stringResource(R.string.local_share_saf_folder))
            }
            Text(
                text = if (safGranted) stringResource(R.string.local_share_saf_granted) else stringResource(R.string.local_share_saf_missing),
                style = MaterialTheme.typography.bodySmall,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp, max = 220.dp).padding(12.dp)) {
                    Text(stringResource(R.string.local_share_received), style = MaterialTheme.typography.titleSmall)
                    if (state.receivedFiles.isEmpty()) {
                        Text(stringResource(R.string.local_share_no_files), style = MaterialTheme.typography.bodySmall)
                    } else {
                        state.receivedFiles.take(12).forEach { name ->
                            Text(name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun generatePin(): String = (100000..999999).random().toString()
