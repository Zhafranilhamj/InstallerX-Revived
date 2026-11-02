// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal

import com.rosan.installer.domain.virustotal.model.VirusTotalDetection
import com.rosan.installer.domain.virustotal.model.VirusTotalFalsePositiveHeuristics
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VirusTotalFalsePositiveHeuristicsTest {
    private fun evaluate(
        positives: Int,
        total: Int,
        detections: List<VirusTotalDetection> = emptyList(),
    ): Boolean = VirusTotalFalsePositiveHeuristics.isLikelyFalsePositive(
        positives = positives,
        total = total,
        detections = detections,
    )

    @Test
    fun `a clean file is never a false positive`() {
        assertFalse(evaluate(positives = 0, total = 70))
    }

    @Test
    fun `a single detection out of many engines is low confidence`() {
        assertTrue(
            evaluate(
                positives = 1,
                total = 70,
                detections = listOf(VirusTotalDetection("Kaspersky", "Trojan.AndroidOS.Boogr.gsh")),
            ),
        )
    }

    @Test
    fun `a concrete malware family is never dismissed as noise`() {
        assertFalse(
            evaluate(
                positives = 1,
                total = 70,
                detections = listOf(VirusTotalDetection("Kaspersky", "Trojan-Banker.AndroidOS.Agent.abc")),
            ),
        )
    }

    @Test
    fun `a few detections out of many engines are low confidence`() {
        assertTrue(evaluate(positives = 4, total = 70))
    }

    @Test
    fun `a broad detection spread stays a real verdict`() {
        assertFalse(evaluate(positives = 12, total = 70))
    }

    @Test
    fun `generic heuristic names stay low confidence in small engine pools`() {
        assertTrue(
            evaluate(
                positives = 2,
                total = 10,
                detections = listOf(
                    VirusTotalDetection("EngineA", "PUA.AndroidOS.Generic"),
                    VirusTotalDetection("EngineB", "Riskware"),
                ),
            ),
        )
    }

    @Test
    fun `concrete malware names are not dismissed in small engine pools`() {
        assertFalse(
            evaluate(
                positives = 2,
                total = 10,
                detections = listOf(
                    VirusTotalDetection("EngineA", "Android.Banker.CredentialStealer"),
                    VirusTotalDetection("EngineB", "Android.Spy.SmsThief"),
                ),
            ),
        )
    }

    @Test
    fun `detections without a reported name are not treated as generic`() {
        assertFalse(
            evaluate(
                positives = 2,
                total = 10,
                detections = listOf(VirusTotalDetection("EngineA"), VirusTotalDetection("EngineB")),
            ),
        )
    }
}
