// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.rosan.installer.ui.util.formatSize

/**
 * VirusTotal card shown on the install confirmation screen.
 *
 * The card is hidden when scanning is Off. In Normal mode both scan actions are optional. In
 * Strict mode the upload scan starts automatically and the install button is blocked until it
 * finishes; a failed scan still allows installation with an explicit caution warning.
 */
@Composable
fun VirusTotalScanCard(
    state: VirusTotalScanState,
    onScanByHash: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.mode == VirusTotalScanMode.OFF) return

    val uriHandler = LocalUriHandler.current
    val isStrict = state.mode == VirusTotalScanMode.STRICT
    val canScan = state.hasUsableApiKey && state.networkEnabled && !state.isScanning

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (state.isLikelyFalsePositive) {
            virusTotalFalsePositiveCardColor(
                base = MaterialTheme.colorScheme.surfaceContainerHighest,
                onSurface = MaterialTheme.colorScheme.surface,
            )
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.virustotal_scan_status_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (isStrict) {
                        stringResource(R.string.virustotal_scan_mode_strict)
                    } else {
                        stringResource(R.string.virustotal_scan_mode_normal)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (!state.hasApiKey) {
                Text(
                    text = stringResource(R.string.virustotal_no_api_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (!state.hasUsableApiKey) {
                Text(
                    text = stringResource(R.string.virustotal_scan_invalid_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (!state.networkEnabled) {
                Text(
                    text = stringResource(R.string.virustotal_network_disabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanByHash,
                    enabled = canScan,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.virustotal_scan_button))
                }
                Button(
                    onClick = onUpload,
                    enabled = canScan,
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
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (state.method == VirusTotalScanMethod.UPLOAD) {
                            stringResource(R.string.virustotal_scanning_upload)
                        } else {
                            stringResource(R.string.virustotal_scanning_hash)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isStrict && state.status == VirusTotalScanStatus.IDLE) {
                Text(
                    text = stringResource(R.string.virustotal_strict_scan_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isStrict && state.status == VirusTotalScanStatus.IDLE) {
                Text(
                    text = stringResource(R.string.virustotal_scan_optional_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.report?.let { report ->
                VirusTotalReportView(
                    report = report,
                    strictMode = isStrict,
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (isStrict && state.status == VirusTotalScanStatus.COMPLETED) {
                // A real malicious verdict keeps the install button disabled; the card explains why.
                Text(
                    text = stringResource(
                        if (state.isBlockedByMalware) {
                            R.string.virustotal_strict_scan_blocked
                        } else {
                            R.string.virustotal_strict_scan_complete
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.isBlockedByMalware) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Bold,
                )
            }

            if (isStrict && state.status == VirusTotalScanStatus.FAILED) {
                Text(
                    text = stringResource(R.string.virustotal_scan_install_with_caution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun VirusTotalReportView(
    report: VirusTotalReport,
    strictMode: Boolean,
    onOpenReport: (String) -> Unit,
) {
    val falsePositiveColor = virusTotalFalsePositiveColor(MaterialTheme.colorScheme.surface)
    val verdictColor = when (report.verdict) {
        VirusTotalVerdict.CLEAN -> MaterialTheme.colorScheme.primary
        VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> falsePositiveColor
        VirusTotalVerdict.SUSPICIOUS -> MaterialTheme.colorScheme.tertiary
        VirusTotalVerdict.MALICIOUS -> MaterialTheme.colorScheme.error
        VirusTotalVerdict.NOT_FOUND -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val verdictIcon: ImageVector = when (report.verdict) {
        VirusTotalVerdict.CLEAN -> Icons.Default.CheckCircle
        VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> Icons.Default.Warning
        VirusTotalVerdict.SUSPICIOUS -> Icons.Default.Warning
        VirusTotalVerdict.MALICIOUS -> Icons.Default.Error
        VirusTotalVerdict.NOT_FOUND -> Icons.Default.Info
    }
    val verdictText = when (report.verdict) {
        VirusTotalVerdict.CLEAN -> stringResource(R.string.virustotal_result_clean)
        VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> stringResource(R.string.virustotal_result_false_positive)
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
                style = MaterialTheme.typography.titleSmall,
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Show which engines flagged the file and the name each of them reported, so the user can
        // judge whether the detections are a real threat or a common false positive.
        if (report.detections.isNotEmpty()) {
            Text(
                text = stringResource(R.string.virustotal_detections_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            report.detections.take(MAX_LISTED_DETECTIONS).forEach { detection ->
                Text(
                    text = detection.displayText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val remaining = report.detections.size - MAX_LISTED_DETECTIONS
            if (remaining > 0) {
                Text(
                    text = stringResource(R.string.virustotal_more_engines, remaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            report.permalink?.let { permalink ->
                TextButton(onClick = { onOpenReport(permalink) }) {
                    Text(text = stringResource(R.string.virustotal_view_report))
                }
            }
        }

        report.sha256?.let { sha256 ->
            Text(
                text = stringResource(
                    R.string.virustotal_sha256,
                    sha256.take(20) + "…",
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val fileInfo = listOfNotNull(
            report.fileName,
            report.fileSize?.takeIf { it > 0 }?.formatSize(),
        ).joinToString(" · ")
        if (fileInfo.isNotBlank()) {
            Text(
                text = fileInfo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (report.verdict) {
            VirusTotalVerdict.MALICIOUS -> {
                Text(
                    text = stringResource(R.string.virustotal_scan_malicious_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> {
                Text(
                    text = stringResource(R.string.virustotal_false_positive_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = falsePositiveColor,
                )
                if (strictMode) {
                    Text(
                        text = stringResource(R.string.virustotal_false_positive_strict_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            VirusTotalVerdict.SUSPICIOUS -> {
                Text(
                    text = stringResource(R.string.virustotal_scan_suspicious_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            VirusTotalVerdict.CLEAN,
            VirusTotalVerdict.NOT_FOUND,
            -> Unit
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
