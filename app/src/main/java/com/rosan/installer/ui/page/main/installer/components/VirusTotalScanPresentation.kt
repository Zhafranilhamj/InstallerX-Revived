// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.domain.virustotal.model.VirusTotalDetection
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import com.rosan.installer.ui.page.main.installer.VirusTotalScanState
import com.rosan.installer.ui.page.main.installer.VirusTotalScanStatus

/** How many engine detections are listed before the rest are collapsed into a counter. */
internal const val MAX_LISTED_DETECTIONS = 5

/** Amber remains a caution state; only the false-positive heading is relabelled as suspicious. */
fun virusTotalFalsePositiveColor(onSurface: Color): Color = if (onSurface.luminance() < 0.5f) Color(0xFFFFC857) else Color(0xFF9A6B00)

fun virusTotalFalsePositiveCardColor(base: Color, onSurface: Color): Color {
    val tint = if (onSurface.luminance() < 0.5f) Color(0xFFFFC107) else Color(0xFFFFD54F)
    return lerp(base, tint, FALSE_POSITIVE_CARD_TINT)
}

private const val FALSE_POSITIVE_CARD_TINT = 0.22f

@Composable
fun VirusTotalDetection.displayText(): String {
    val detectionName = name?.takeIf { it.isNotBlank() } ?: return engine
    return stringResource(R.string.virustotal_detection_engine_result, engine, detectionName)
}

@Composable
internal fun VirusTotalVerdict.displayLabel(): String = stringResource(
    when (this) {
        VirusTotalVerdict.CLEAN -> R.string.virustotal_result_clean
        VirusTotalVerdict.LIKELY_FALSE_POSITIVE -> R.string.virustotal_result_false_positive
        VirusTotalVerdict.SUSPICIOUS -> R.string.virustotal_result_suspicious
        VirusTotalVerdict.MALICIOUS -> R.string.virustotal_result_malicious
        VirusTotalVerdict.NOT_FOUND -> R.string.virustotal_result_not_found
    },
)

@Composable
internal fun VirusTotalReport.detectionSummary(): String = if (positives > 0) {
    stringResource(R.string.virustotal_detections, positives, total)
} else {
    stringResource(R.string.virustotal_no_detections, total)
}

@Composable
internal fun VirusTotalScanState.compactStatus(): String = when {
    !networkEnabled -> stringResource(R.string.virustotal_network_disabled)

    !hasApiKey -> stringResource(R.string.virustotal_no_api_key)

    !hasUsableApiKey -> stringResource(R.string.virustotal_scan_invalid_key)

    isScanning -> stringResource(
        if (method == VirusTotalScanMethod.UPLOAD) R.string.virustotal_scanning_upload else R.string.virustotal_scanning_hash,
    )

    isBlockedByMalware -> stringResource(R.string.virustotal_strict_scan_blocked)

    isStrict && status == VirusTotalScanStatus.FAILED -> stringResource(R.string.virustotal_scan_install_with_caution)

    report != null -> report.verdict.displayLabel()

    error != null -> error.kind.messageForDisplay()

    isStrict && status == VirusTotalScanStatus.IDLE -> stringResource(R.string.virustotal_strict_scan_required)

    else -> stringResource(R.string.virustotal_scan_ready)
}

@Composable
internal fun VirusTotalErrorKind.messageForDisplay(): String = stringResource(
    when (this) {
        VirusTotalErrorKind.SCAN_DISABLED -> R.string.virustotal_scan_disabled
        VirusTotalErrorKind.COMMUNITY_UNAVAILABLE -> R.string.virustotal_community_unavailable
        VirusTotalErrorKind.VOTE_FAILED -> R.string.virustotal_vote_failed
        VirusTotalErrorKind.VOTE_REFRESH_FAILED -> R.string.virustotal_vote_refresh_failed
        VirusTotalErrorKind.NO_API_KEY -> R.string.virustotal_no_api_key
        VirusTotalErrorKind.INVALID_API_KEY -> R.string.virustotal_scan_invalid_key
        VirusTotalErrorKind.NETWORK_ERROR -> R.string.virustotal_scan_network_error
        VirusTotalErrorKind.FILE_TOO_LARGE -> R.string.virustotal_scan_file_too_large
        VirusTotalErrorKind.FILE_UNREADABLE -> R.string.virustotal_scan_file_unreadable
        VirusTotalErrorKind.NOT_FOUND -> R.string.virustotal_scan_not_found
        VirusTotalErrorKind.QUOTA_EXCEEDED -> R.string.virustotal_scan_quota_exceeded
        VirusTotalErrorKind.TIMEOUT -> R.string.virustotal_scan_timeout
        VirusTotalErrorKind.UNKNOWN -> R.string.virustotal_scan_unknown_error
    },
)
