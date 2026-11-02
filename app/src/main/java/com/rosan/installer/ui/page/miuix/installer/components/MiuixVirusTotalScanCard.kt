// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import com.rosan.installer.ui.page.main.installer.VirusTotalScanState
import com.rosan.installer.ui.page.main.installer.VirusTotalScanStatus
import com.rosan.installer.ui.theme.miuixSheetCardColors
import com.rosan.installer.ui.util.formatSize
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix variant of the VirusTotal install confirmation card.
 */
@Composable
fun MiuixVirusTotalScanCard(
    state: VirusTotalScanState,
    onScanByHash: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.mode == VirusTotalScanMode.OFF) return

    val uriHandler = LocalUriHandler.current
    val isStrict = state.mode == VirusTotalScanMode.STRICT
    val canScan = state.hasApiKey && state.networkEnabled && !state.isScanning

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = miuixSheetCardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.virustotal_scan_status_title),
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (isStrict) {
                        stringResource(R.string.virustotal_scan_mode_strict)
                    } else {
                        stringResource(R.string.virustotal_scan_mode_normal)
                    },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }

            if (!state.hasApiKey) {
                Text(
                    text = stringResource(R.string.virustotal_no_api_key),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                )
            } else if (!state.networkEnabled) {
                Text(
                    text = stringResource(R.string.virustotal_network_disabled),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanByHash,
                    enabled = canScan,
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.virustotal_scan_button))
                }
                Button(
                    onClick = onUpload,
                    enabled = canScan,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.virustotal_upload_button))
                }
            }

            if (state.isScanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MiuixTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (state.method == VirusTotalScanMethod.UPLOAD) {
                            stringResource(R.string.virustotal_scanning_upload)
                        } else {
                            stringResource(R.string.virustotal_scanning_hash)
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            if (isStrict && state.status == VirusTotalScanStatus.IDLE) {
                Text(
                    text = stringResource(R.string.virustotal_strict_scan_required),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            if (!isStrict && state.status == VirusTotalScanStatus.IDLE) {
                Text(
                    text = stringResource(R.string.virustotal_scan_optional_hint),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            state.report?.let { report ->
                MiuixVirusTotalReportView(
                    report = report,
                    onOpenReport = { permalink ->
                        runCatching { uriHandler.openUri(permalink) }
                    },
                )
            }

            state.error?.let { error ->
                val alreadyExplained = (error.kind == VirusTotalErrorKind.NO_API_KEY && !state.hasApiKey) ||
                    (error.kind == VirusTotalErrorKind.NETWORK_ERROR && !state.networkEnabled)
                if (!alreadyExplained) {
                    Text(
                        text = error.kind.messageForDisplay(),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }

            if (isStrict && state.status == VirusTotalScanStatus.COMPLETED) {
                Text(
                    text = stringResource(R.string.virustotal_strict_scan_complete),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (isStrict && state.status == VirusTotalScanStatus.FAILED) {
                Text(
                    text = stringResource(R.string.virustotal_scan_install_with_caution),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MiuixVirusTotalReportView(
    report: VirusTotalReport,
    onOpenReport: (String) -> Unit,
) {
    val verdictColor: Color = when (report.verdict) {
        VirusTotalVerdict.CLEAN -> MiuixTheme.colorScheme.primary
        VirusTotalVerdict.SUSPICIOUS -> MiuixTheme.colorScheme.tertiaryContainer
        VirusTotalVerdict.MALICIOUS -> MiuixTheme.colorScheme.error
        VirusTotalVerdict.NOT_FOUND -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val verdictIcon: ImageVector = when (report.verdict) {
        VirusTotalVerdict.CLEAN -> Icons.Default.CheckCircle
        VirusTotalVerdict.SUSPICIOUS -> Icons.Default.Warning
        VirusTotalVerdict.MALICIOUS -> Icons.Default.Error
        VirusTotalVerdict.NOT_FOUND -> Icons.Default.Info
    }
    val verdictText = when (report.verdict) {
        VirusTotalVerdict.CLEAN -> stringResource(R.string.virustotal_result_clean)
        VirusTotalVerdict.SUSPICIOUS -> stringResource(R.string.virustotal_result_suspicious)
        VirusTotalVerdict.MALICIOUS -> stringResource(R.string.virustotal_result_malicious)
        VirusTotalVerdict.NOT_FOUND -> stringResource(R.string.virustotal_result_not_found)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = verdictIcon,
                contentDescription = null,
                tint = verdictColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = verdictText,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Bold,
                color = verdictColor,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (report.positives > 0) {
                    stringResource(
                        R.string.virustotal_detections,
                        report.positives,
                        report.total,
                    )
                } else {
                    stringResource(R.string.virustotal_no_detections, report.total)
                },
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        if (report.detections.isNotEmpty()) {
            val visible = report.detections.take(6)
            val remaining = report.detections.size - visible.size
            Text(
                text = visible.joinToString(", ") +
                    if (remaining > 0) stringResource(R.string.virustotal_more_engines, remaining) else "",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.virustotal_scan_method,
                    if (report.method == VirusTotalScanMethod.HASH) {
                        stringResource(R.string.virustotal_scan_method_hash)
                    } else {
                        stringResource(R.string.virustotal_scan_method_upload)
                    },
                ),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.weight(1f),
            )
            report.permalink?.let { permalink ->
                TextButton(
                    onClick = { onOpenReport(permalink) },
                    text = stringResource(R.string.virustotal_view_report),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }

        report.sha256?.let { sha256 ->
            Text(
                text = stringResource(
                    R.string.virustotal_sha256,
                    sha256.take(20) + "…",
                ),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        val fileInfo = listOfNotNull(
            report.fileName,
            report.fileSize?.takeIf { it > 0 }?.formatSize(),
        ).joinToString(" · ")
        if (fileInfo.isNotBlank()) {
            Text(
                text = fileInfo,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        if (report.verdict == VirusTotalVerdict.MALICIOUS) {
            Text(
                text = stringResource(R.string.virustotal_scan_malicious_warning),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.error,
            )
        } else if (report.verdict == VirusTotalVerdict.SUSPICIOUS) {
            Text(
                text = stringResource(R.string.virustotal_scan_suspicious_warning),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.tertiaryContainer,
            )
        }
    }
}

@Composable
private fun VirusTotalErrorKind.messageForDisplay(): String {
    val text = when (this) {
        VirusTotalErrorKind.NO_API_KEY -> R.string.virustotal_no_api_key
        VirusTotalErrorKind.INVALID_API_KEY -> R.string.virustotal_scan_invalid_key
        VirusTotalErrorKind.NETWORK_ERROR -> R.string.virustotal_scan_network_error
        VirusTotalErrorKind.FILE_TOO_LARGE -> R.string.virustotal_scan_file_too_large
        VirusTotalErrorKind.FILE_UNREADABLE -> R.string.virustotal_scan_file_unreadable
        VirusTotalErrorKind.NOT_FOUND -> R.string.virustotal_scan_not_found
        VirusTotalErrorKind.QUOTA_EXCEEDED -> R.string.virustotal_scan_quota_exceeded
        VirusTotalErrorKind.TIMEOUT -> R.string.virustotal_scan_timeout
        VirusTotalErrorKind.UNKNOWN -> R.string.virustotal_scan_unknown_error
    }
    return stringResource(text)
}
