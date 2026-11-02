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

/** How many engine detections are listed before the rest are collapsed into a counter. */
internal const val MAX_LISTED_DETECTIONS = 5

/**
 * Amber used for the "likely false positive" VirusTotal state.
 *
 * The tone is chosen from the surface it is painted on so the warning stays readable in both light
 * and dark themes. This is a plain function instead of a composable so both UI families can use it.
 */
fun virusTotalFalsePositiveColor(onSurface: Color): Color = if (onSurface.luminance() < 0.5f) Color(0xFFFFC857) else Color(0xFF9A6B00)

/**
 * The card container colour for the "likely false positive" state: the regular card colour with a
 * subtle amber wash so the whole result card reads as caution instead of threat.
 */
fun virusTotalFalsePositiveCardColor(base: Color, onSurface: Color): Color {
    val tint = if (onSurface.luminance() < 0.5f) Color(0xFFFFC107) else Color(0xFFFFD54F)
    return lerp(base, tint, FALSE_POSITIVE_CARD_TINT)
}

private const val FALSE_POSITIVE_CARD_TINT = 0.22f

/**
 * "Engine: detection name" for one VirusTotal detection, falling back to the engine name when the
 * engine did not report a signature name.
 */
@Composable
fun VirusTotalDetection.displayText(): String {
    val detectionName = name?.takeIf { it.isNotBlank() } ?: return engine
    return stringResource(R.string.virustotal_detection_engine_result, engine, detectionName)
}
