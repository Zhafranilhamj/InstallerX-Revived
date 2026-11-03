// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityVerdict
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import com.rosan.installer.ui.page.main.installer.VirusTotalScanState
import com.rosan.installer.ui.page.main.installer.VirusTotalScanStatus
import com.rosan.installer.ui.util.formatSize

/** Compact idle status plus two independent action cards. Scanning expands the status automatically. */
@Composable
fun VirusTotalScanCard(
    state: VirusTotalScanState,
    onScanByHash: () -> Unit,
    onUpload: () -> Unit,
    onVote: (VirusTotalCommunityVerdict) -> Unit,
    onRetryCommunity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.mode == VirusTotalScanMode.OFF) return
    val uriHandler = LocalUriHandler.current
    val isStrict = state.isStrict
    val canScan = state.hasUsableApiKey && state.networkEnabled && !state.isScanning
    var expanded by rememberSaveable(state.packageName, state.report?.sha256, state.status) { mutableStateOf(false) }
    val showDetails = state.isScanning || expanded
    val expandDescription = stringResource(
        if (showDetails) R.string.virustotal_collapse_details else R.string.virustotal_expand_details,
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = { if (!state.isScanning) expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).semantics { contentDescription = expandDescription },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = state.report?.verdict.icon() ?: Icons.Default.Security,
                    contentDescription = null,
                    tint = state.report?.verdict.color() ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.virustotal_scan_status_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = state.compactStatus(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isBlockedByMalware) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (state.isBlockedByMalware || (isStrict && state.status == VirusTotalScanStatus.FAILED)) Int.MAX_VALUE else 2,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(if (isStrict) R.string.virustotal_scan_mode_strict else R.string.virustotal_scan_mode_normal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (state.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp).size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
            }
        }

        AnimatedVisibility(visible = !state.isScanning) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VirusTotalActionCard(
                    text = stringResource(R.string.virustotal_scan_button),
                    icon = Icons.Default.Search,
                    enabled = canScan,
                    onClick = onScanByHash,
                    modifier = Modifier.weight(1f),
                )
                VirusTotalActionCard(
                    text = stringResource(R.string.virustotal_upload_button),
                    icon = Icons.Default.UploadFile,
                    enabled = canScan,
                    onClick = onUpload,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedVisibility(visible = showDetails) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (state.isLikelyFalsePositive) {
                    virusTotalFalsePositiveCardColor(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surface)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!state.networkEnabled) {
                        Text(stringResource(R.string.virustotal_network_disabled), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else if (!state.hasApiKey) {
                        Text(stringResource(R.string.virustotal_no_api_key), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else if (!state.hasUsableApiKey) {
                        Text(stringResource(R.string.virustotal_scan_invalid_key), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.status == VirusTotalScanStatus.IDLE) {
                        Text(
                            text = stringResource(if (isStrict) R.string.virustotal_strict_scan_required else R.string.virustotal_scan_optional_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.report?.let { report ->
                        VirusTotalReportView(
                            report = report,
                            state = state,
                            onOpenReport = { runCatching { uriHandler.openUri(it) } },
                            onVote = onVote,
                            onRetryCommunity = onRetryCommunity,
                        )
                    }
                    state.error?.let { error ->
                        val alreadyExplained = (error.kind == VirusTotalErrorKind.NO_API_KEY && !state.hasApiKey) ||
                            (error.kind == VirusTotalErrorKind.NETWORK_ERROR && !state.networkEnabled)
                        if (!alreadyExplained) {
                            Text(error.kind.messageForDisplay(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (isStrict && state.status == VirusTotalScanStatus.COMPLETED) {
                        Text(
                            text = stringResource(if (state.isBlockedByMalware) R.string.virustotal_strict_scan_blocked else R.string.virustotal_strict_scan_complete),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isBlockedByMalware) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
    }
}

@Composable
private fun VirusTotalActionCard(text: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 76.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun VirusTotalReportView(
    report: VirusTotalReport,
    state: VirusTotalScanState,
    onOpenReport: (String) -> Unit,
    onVote: (VirusTotalCommunityVerdict) -> Unit,
    onRetryCommunity: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(report.verdict.icon(), null, tint = report.verdict.color(), modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(report.verdict.displayLabel(), style = MaterialTheme.typography.titleSmall, color = report.verdict.color(), modifier = Modifier.weight(1f))
        }
        Text(report.detectionSummary(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (report.detections.isNotEmpty()) {
            Text(stringResource(R.string.virustotal_detections_title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            report.detections.take(MAX_LISTED_DETECTIONS).forEach {
                Text(it.displayText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val remaining = report.detections.size - MAX_LISTED_DETECTIONS
            if (remaining > 0) Text(stringResource(R.string.virustotal_more_detections, remaining), style = MaterialTheme.typography.labelSmall)
        }
        VirusTotalVerdictWarning(report, state.isStrict)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        VirusTotalCommunityScoresView(state, onVote, onRetryCommunity)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.virustotal_scan_method,
                    stringResource(if (report.method == VirusTotalScanMethod.HASH) R.string.virustotal_scan_method_hash else R.string.virustotal_scan_method_upload),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            report.permalink?.let { permalink ->
                TextButton(onClick = { onOpenReport(permalink) }) { Text(stringResource(R.string.virustotal_view_report)) }
            }
        }
        report.sha256?.let {
            Text(stringResource(R.string.virustotal_sha256, it.take(20) + "…"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val fileInfo = listOfNotNull(report.fileName, report.fileSize?.takeIf { it > 0 }?.formatSize()).joinToString(" · ")
        if (fileInfo.isNotBlank()) Text(fileInfo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VirusTotalVerdictWarning(report: VirusTotalReport, strictMode: Boolean) {
    when (report.verdict) {
        VirusTotalVerdict.MALICIOUS -> Text(
            stringResource(R.string.virustotal_scan_malicious_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> {
            // Keep the original caution message unchanged; only its heading is called Suspicious.
            Text(
                stringResource(R.string.virustotal_false_positive_warning),
                style = MaterialTheme.typography.bodySmall,
                color = virusTotalFalsePositiveColor(MaterialTheme.colorScheme.surface),
            )
            if (strictMode) Text(
                stringResource(R.string.virustotal_false_positive_strict_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        VirusTotalVerdict.SUSPICIOUS -> Text(
            stringResource(R.string.virustotal_scan_suspicious_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        VirusTotalVerdict.CLEAN, VirusTotalVerdict.NOT_FOUND -> Unit
    }
}

@Composable
private fun VirusTotalCommunityScoresView(state: VirusTotalScanState, onVote: (VirusTotalCommunityVerdict) -> Unit, onRetryCommunity: () -> Unit) {
    val scores = state.report?.communityScores
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.virustotal_community_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (state.isCommunityLoading || state.isVoteSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else if (scores == null || state.communityError != null) {
                TextButton(onClick = onRetryCommunity, enabled = state.networkEnabled && state.hasUsableApiKey) {
                    Text(stringResource(R.string.virustotal_community_retry))
                }
            }
        }
        if (scores != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.virustotal_community_harmless, scores.harmless), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.virustotal_community_malicious, scores.malicious), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Text(stringResource(R.string.virustotal_community_reputation, scores.reputation), style = MaterialTheme.typography.labelSmall)
        } else if (state.isCommunityLoading) {
            Text(stringResource(R.string.virustotal_community_loading), style = MaterialTheme.typography.bodySmall)
        }
        state.communityError?.let {
            Text(it.kind.messageForDisplay(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CommunityVoteButton(VirusTotalCommunityVerdict.HARMLESS, state, onVote, Modifier.weight(1f))
            CommunityVoteButton(VirusTotalCommunityVerdict.MALICIOUS, state, onVote, Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.virustotal_vote_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommunityVoteButton(verdict: VirusTotalCommunityVerdict, state: VirusTotalScanState, onVote: (VirusTotalCommunityVerdict) -> Unit, modifier: Modifier = Modifier) {
    val harmless = verdict == VirusTotalCommunityVerdict.HARMLESS
    val selected = state.submittedVote == verdict
    OutlinedButton(
        onClick = { state.report?.sha256?.let { onVote(verdict) } },
        enabled = state.canVote,
        modifier = modifier.heightIn(min = 40.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.Check else if (harmless) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = stringResource(if (harmless) R.string.virustotal_vote_harmless else R.string.virustotal_vote_malicious),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VirusTotalVerdict.color(): Color = when (this) {
    VirusTotalVerdict.CLEAN -> MaterialTheme.colorScheme.primary
    VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> virusTotalFalsePositiveColor(MaterialTheme.colorScheme.surface)
    VirusTotalVerdict.SUSPICIOUS -> MaterialTheme.colorScheme.tertiary
    VirusTotalVerdict.MALICIOUS -> MaterialTheme.colorScheme.error
    VirusTotalVerdict.NOT_FOUND -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun VirusTotalVerdict.icon(): ImageVector = when (this) {
    VirusTotalVerdict.CLEAN -> Icons.Default.CheckCircle
    VirusTotalVerdict.LIKELY_FALSE_POSITIVE, VirusTotalVerdict.SUSPICIOUS -> Icons.Default.Warning
    VirusTotalVerdict.MALICIOUS -> Icons.Default.Error
    VirusTotalVerdict.NOT_FOUND -> Icons.Default.Info
}
