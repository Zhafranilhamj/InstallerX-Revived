// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.components

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
import androidx.compose.ui.semantics.disabled
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
import com.rosan.installer.ui.page.main.installer.components.MAX_LISTED_DETECTIONS
import com.rosan.installer.ui.page.main.installer.components.compactStatus
import com.rosan.installer.ui.page.main.installer.components.detectionSummary
import com.rosan.installer.ui.page.main.installer.components.displayLabel
import com.rosan.installer.ui.page.main.installer.components.displayText
import com.rosan.installer.ui.page.main.installer.components.messageForDisplay
import com.rosan.installer.ui.page.main.installer.components.virusTotalFalsePositiveCardColor
import com.rosan.installer.ui.page.main.installer.components.virusTotalFalsePositiveColor
import com.rosan.installer.ui.theme.miuixSheetCardColor
import com.rosan.installer.ui.theme.miuixSheetCardColors
import com.rosan.installer.ui.util.formatSize
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** Miuix-native compact cards, sharing scan state and safety rules with Material 3. */
@Composable
fun MiuixVirusTotalScanCard(
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
    val expandDescription = stringResource(if (showDetails) R.string.virustotal_collapse_details else R.string.virustotal_expand_details)

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).semantics { contentDescription = expandDescription },
            insideMargin = PaddingValues(12.dp),
            onClick = { if (!state.isScanning) expanded = !expanded },
            pressFeedbackType = PressFeedbackType.Sink,
            colors = miuixSheetCardColors(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = state.report?.verdict?.icon() ?: Icons.Default.Security,
                    contentDescription = null,
                    tint = state.report?.verdict?.color() ?: MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.virustotal_scan_status_title), style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
                    Text(
                        text = state.compactStatus(),
                        style = MiuixTheme.textStyles.footnote1,
                        color = if (state.isBlockedByMalware) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = if (state.isBlockedByMalware || (isStrict && state.status == VirusTotalScanStatus.FAILED)) Int.MAX_VALUE else 2,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(if (isStrict) R.string.virustotal_scan_mode_strict else R.string.virustotal_scan_mode_normal),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
                if (state.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp).size(18.dp), strokeWidth = 2.dp, color = MiuixTheme.colorScheme.primary)
                } else {
                    Icon(if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
            }
        }

        AnimatedVisibility(visible = !state.isScanning) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiuixVirusTotalActionCard(stringResource(R.string.virustotal_scan_button), Icons.Default.Search, canScan, onScanByHash, Modifier.weight(1f))
                MiuixVirusTotalActionCard(stringResource(R.string.virustotal_upload_button), Icons.Default.UploadFile, canScan, onUpload, Modifier.weight(1f))
            }
        }

        AnimatedVisibility(visible = showDetails) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(12.dp),
                colors = if (state.isLikelyFalsePositive) {
                    CardColors(
                        color = virusTotalFalsePositiveCardColor(miuixSheetCardColor(), MiuixTheme.colorScheme.onSurface),
                        contentColor = MiuixTheme.colorScheme.onSurface,
                    )
                } else {
                    miuixSheetCardColors()
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!state.networkEnabled) {
                        Text(stringResource(R.string.virustotal_network_disabled), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.error)
                    } else if (!state.hasApiKey) {
                        Text(stringResource(R.string.virustotal_no_api_key), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.error)
                    } else if (!state.hasUsableApiKey) {
                        Text(stringResource(R.string.virustotal_scan_invalid_key), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.error)
                    }
                    if (state.status == VirusTotalScanStatus.IDLE) {
                        Text(
                            text = stringResource(if (isStrict) R.string.virustotal_strict_scan_required else R.string.virustotal_scan_optional_hint),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    state.report?.let { report ->
                        MiuixVirusTotalReportView(
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
                        if (!alreadyExplained) Text(error.kind.messageForDisplay(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.error)
                    }
                    if (isStrict && state.status == VirusTotalScanStatus.COMPLETED) {
                        Text(
                            text = stringResource(if (state.isBlockedByMalware) R.string.virustotal_strict_scan_blocked else R.string.virustotal_strict_scan_complete),
                            style = MiuixTheme.textStyles.body2,
                            color = if (state.isBlockedByMalware) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
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
    }
}

@Composable
private fun MiuixVirusTotalActionCard(text: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.heightIn(min = 76.dp).semantics { if (!enabled) disabled() },
        insideMargin = PaddingValues(10.dp),
        onClick = if (enabled) onClick else null,
        pressFeedbackType = if (enabled) PressFeedbackType.Sink else PressFeedbackType.None,
        colors = CardColors(
            color = miuixSheetCardColor(),
            contentColor = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = MiuixTheme.textStyles.footnote1, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MiuixVirusTotalReportView(
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
            Text(report.verdict.displayLabel(), style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, color = report.verdict.color(), modifier = Modifier.weight(1f))
        }
        Text(report.detectionSummary(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (report.detections.isNotEmpty()) {
            Text(stringResource(R.string.virustotal_detections_title), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            report.detections.take(MAX_LISTED_DETECTIONS).forEach {
                Text(it.displayText(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            val remaining = report.detections.size - MAX_LISTED_DETECTIONS
            if (remaining > 0) Text(stringResource(R.string.virustotal_more_detections, remaining), style = MiuixTheme.textStyles.body2)
        }
        MiuixVirusTotalVerdictWarning(report, state.isStrict)
        MiuixVirusTotalCommunityScoresView(state, onVote, onRetryCommunity)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.virustotal_scan_method,
                    stringResource(if (report.method == VirusTotalScanMethod.HASH) R.string.virustotal_scan_method_hash else R.string.virustotal_scan_method_upload),
                ),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.weight(1f),
            )
            report.permalink?.let { permalink ->
                TextButton(onClick = { onOpenReport(permalink) }, text = stringResource(R.string.virustotal_view_report), colors = ButtonDefaults.textButtonColorsPrimary())
            }
        }
        report.sha256?.let {
            Text(stringResource(R.string.virustotal_sha256, it.take(20) + "…"), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        val fileInfo = listOfNotNull(report.fileName, report.fileSize?.takeIf { it > 0 }?.formatSize()).joinToString(" · ")
        if (fileInfo.isNotBlank()) Text(fileInfo, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun MiuixVirusTotalVerdictWarning(report: VirusTotalReport, strictMode: Boolean) {
    when (report.verdict) {
        VirusTotalVerdict.MALICIOUS -> Text(stringResource(R.string.virustotal_scan_malicious_warning), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.error)

        VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> {
            // The original caution message is intentionally unchanged.
            Text(
                stringResource(R.string.virustotal_false_positive_warning),
                style = MiuixTheme.textStyles.body2,
                color = virusTotalFalsePositiveColor(MiuixTheme.colorScheme.onSurface),
            )
            if (strictMode) {
                Text(
                    stringResource(R.string.virustotal_false_positive_strict_note),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        VirusTotalVerdict.SUSPICIOUS -> Text(stringResource(R.string.virustotal_scan_suspicious_warning), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.tertiaryContainer)

        VirusTotalVerdict.CLEAN, VirusTotalVerdict.NOT_FOUND -> Unit
    }
}

@Composable
private fun MiuixVirusTotalCommunityScoresView(state: VirusTotalScanState, onVote: (VirusTotalCommunityVerdict) -> Unit, onRetryCommunity: () -> Unit) {
    val scores = state.report?.communityScores
    Column(modifier = Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.virustotal_community_title), style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (state.isCommunityLoading || state.isVoteSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MiuixTheme.colorScheme.primary)
            } else if (scores == null || state.communityError != null) {
                TextButton(
                    onClick = onRetryCommunity,
                    enabled = state.networkEnabled && state.hasUsableApiKey,
                    text = stringResource(R.string.virustotal_community_retry),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
        if (scores != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.virustotal_community_harmless, scores.harmless), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.primary)
                Text(stringResource(R.string.virustotal_community_malicious, scores.malicious), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.error)
            }
            Text(stringResource(R.string.virustotal_community_reputation, scores.reputation), style = MiuixTheme.textStyles.footnote1)
        } else if (state.isCommunityLoading) {
            Text(stringResource(R.string.virustotal_community_loading), style = MiuixTheme.textStyles.body2)
        }
        state.communityError?.let { Text(it.kind.messageForDisplay(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiuixCommunityVoteButton(VirusTotalCommunityVerdict.HARMLESS, state, onVote, Modifier.weight(1f))
            MiuixCommunityVoteButton(VirusTotalCommunityVerdict.MALICIOUS, state, onVote, Modifier.weight(1f))
        }
        Text(stringResource(R.string.virustotal_vote_hint), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun MiuixCommunityVoteButton(verdict: VirusTotalCommunityVerdict, state: VirusTotalScanState, onVote: (VirusTotalCommunityVerdict) -> Unit, modifier: Modifier = Modifier) {
    val harmless = verdict == VirusTotalCommunityVerdict.HARMLESS
    val selected = state.submittedVote == verdict
    Button(
        onClick = { state.report?.sha256?.let { onVote(verdict) } },
        enabled = state.canVote,
        modifier = modifier,
        minHeight = 40.dp,
        minWidth = 0.dp,
        insideMargin = PaddingValues(8.dp),
        colors = if (harmless) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
    ) {
        Icon(
            imageVector = if (selected) {
                Icons.Default.Check
            } else if (harmless) {
                Icons.Default.ThumbUp
            } else {
                Icons.Default.ThumbDown
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = stringResource(if (harmless) R.string.virustotal_vote_harmless else R.string.virustotal_vote_malicious),
            style = MiuixTheme.textStyles.footnote1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VirusTotalVerdict.color(): Color = when (this) {
    VirusTotalVerdict.CLEAN -> MiuixTheme.colorScheme.primary
    VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> virusTotalFalsePositiveColor(MiuixTheme.colorScheme.onSurface)
    VirusTotalVerdict.SUSPICIOUS -> MiuixTheme.colorScheme.tertiaryContainer
    VirusTotalVerdict.MALICIOUS -> MiuixTheme.colorScheme.error
    VirusTotalVerdict.NOT_FOUND -> MiuixTheme.colorScheme.onSurfaceVariantSummary
}

private fun VirusTotalVerdict.icon(): ImageVector = when (this) {
    VirusTotalVerdict.CLEAN -> Icons.Default.CheckCircle
    VirusTotalVerdict.LIKELY_FALSE_POSITIVE, VirusTotalVerdict.SUSPICIOUS -> Icons.Default.Warning
    VirusTotalVerdict.MALICIOUS -> Icons.Default.Error
    VirusTotalVerdict.NOT_FOUND -> Icons.Default.Info
}
