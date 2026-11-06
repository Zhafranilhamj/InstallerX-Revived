// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.appmanager.model.AppListFilter
import com.rosan.installer.domain.appmanager.model.AppListQuery
import com.rosan.installer.domain.appmanager.model.AppSort
import com.rosan.installer.domain.appmanager.model.InstalledAppDetails
import com.rosan.installer.domain.appmanager.model.UninstallState
import com.rosan.installer.domain.appmanager.repository.InstalledAppsRepository
import com.rosan.installer.ui.util.formatSize
import org.koin.compose.koinInject

@Composable
fun AppManagerPage() {
    val repository: InstalledAppsRepository = koinInject()
    var apps by remember { mutableStateOf<List<InstalledAppDetails>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var includeSystem by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(AppSort.NAME) }
    var descending by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var detail by remember { mutableStateOf<InstalledAppDetails?>(null) }
    val log by repository.uninstallLog.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        apps = repository.list()
        loading = false
    }

    val visible = remember(apps, query, includeSystem, sort, descending) {
        AppListQuery.apply(
            apps,
            AppListFilter(
                query = query,
                includeUser = true,
                includeSystem = includeSystem,
                sort = sort,
                descending = descending,
            ),
        )
    }

    ToolsScaffold(title = stringResource(R.string.app_manager_title)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.app_manager_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = includeSystem,
                onClick = { includeSystem = !includeSystem },
                label = { Text(stringResource(R.string.app_manager_system_apps)) },
            )
            FilterChip(
                selected = descending,
                onClick = { descending = !descending },
                label = { Text(stringResource(R.string.app_manager_descending)) },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AppSort.entries) { option ->
                FilterChip(
                    selected = sort == option,
                    onClick = { sort = option },
                    label = { Text(option.label()) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = selected.isNotEmpty(),
                onClick = {
                    repository.uninstall(selected.toList())
                    selected = emptySet()
                },
            ) { Text(stringResource(R.string.app_manager_uninstall, selected.size)) }
            TextButton(onClick = { repository.clearLog() }) { Text(stringResource(R.string.app_manager_clear_log)) }
        }
        if (loading) Text(stringResource(R.string.app_manager_loading))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(visible, key = { it.packageName }) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { detail = app },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = app.packageName in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + app.packageName else selected - app.packageName
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.titleSmall)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${app.versionName.orEmpty()} · SDK ${app.minSdk}-${app.targetSdk} · ${app.apkSize.formatSize()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (app.isSystem) {
                            Text(stringResource(R.string.app_manager_system), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        if (log.isNotEmpty()) {
            Text(stringResource(R.string.app_manager_log), style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)) {
                items(log) { entry ->
                    val stateLabel = when (entry.state) {
                        UninstallState.PENDING -> stringResource(R.string.app_manager_pending)
                        UninstallState.SUCCESS -> stringResource(R.string.app_manager_success)
                        UninstallState.FAILED -> stringResource(R.string.app_manager_failed)
                    }
                    Text("${entry.packageName}: $stateLabel ${entry.detail}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    detail?.let { app ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(app.label) },
            text = {
                Text(
                    buildString {
                        appendLine(app.packageName)
                        appendLine("${app.versionName.orEmpty()} (${app.versionCode})")
                        appendLine("SDK: min ${app.minSdk}, target ${app.targetSdk}")
                        appendLine("Size: ${app.apkSize.formatSize()}")
                        appendLine("Installed: ${app.installTime}")
                        appendLine("Updated: ${app.updateTime}")
                        appendLine("Last used: ${app.lastUsedTime}")
                        appendLine("Installer: ${app.installerPackageName.orEmpty()}")
                        appendLine("Permissions (${app.permissions.size}): ${app.permissions.take(8).joinToString()}")
                        append("Signatures: ${app.signatureSha256.take(2).joinToString { it.take(16) }}")
                    },
                )
            },
            confirmButton = { TextButton(onClick = { detail = null }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun AppSort.label(): String = stringResource(
    when (this) {
        AppSort.NAME -> R.string.app_manager_sort_name
        AppSort.INSTALL_DATE -> R.string.app_manager_sort_install
        AppSort.UPDATE_DATE -> R.string.app_manager_sort_update
        AppSort.SIZE -> R.string.app_manager_sort_size
        AppSort.LAST_USED -> R.string.app_manager_sort_used
    },
)
