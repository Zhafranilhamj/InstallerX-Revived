// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityScores
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VirusTotalScanStateTest {
    private fun state(verdict: VirusTotalVerdict = VirusTotalVerdict.CLEAN, mode: VirusTotalScanMode = VirusTotalScanMode.NORMAL) = VirusTotalScanState(
        mode = mode,
        apiKey = "a".repeat(64),
        networkEnabled = true,
        status = VirusTotalScanStatus.COMPLETED,
        report = VirusTotalReport(
            verdict = verdict,
            sha256 = "b".repeat(64),
            method = VirusTotalScanMethod.HASH,
            communityScores = VirusTotalCommunityScores(7, 2, 5),
        ),
    )

    @Test
    fun `community totals are independent of engine detections`() {
        val scores = state().report!!.communityScores!!
        assertEquals(9L, scores.totalVotes)
        assertEquals(0, state().report!!.positives)
        assertEquals(5, scores.reputation)
    }

    @Test
    fun `voting never weakens the strict malware gate`() {
        val strict = state(VirusTotalVerdict.MALICIOUS, VirusTotalScanMode.STRICT)
        assertTrue(strict.isBlockedByMalware)
        assertTrue(strict.blocksInstallation)
        assertTrue(strict.canVote)
        assertFalse(state(VirusTotalVerdict.MALICIOUS).blocksInstallation)
        assertFalse(state(VirusTotalVerdict.LIKELY_FALSE_POSITIVE, VirusTotalScanMode.STRICT).blocksInstallation)
    }

    @Test
    fun `vote requires a usable key network a report and an idle submission`() {
        assertTrue(state().canVote)
        assertFalse(state().copy(networkEnabled = false).canVote)
        assertFalse(state().copy(mode = VirusTotalScanMode.OFF).canVote)
        assertFalse(state().copy(apiKey = "invalid").canVote)
        assertFalse(state().copy(report = null).canVote)
        assertFalse(state().copy(isVoteSubmitting = true).canVote)
        assertFalse(state().copy(status = VirusTotalScanStatus.SCANNING).canVote)
    }

    @Test
    fun `scanning results and failures auto-expand details while idle stays compact`() {
        val idle = state().copy(status = VirusTotalScanStatus.IDLE, report = null)
        assertFalse(idle.shouldAutoExpandDetails)
        assertTrue(state().copy(status = VirusTotalScanStatus.SCANNING).shouldAutoExpandDetails)
        assertTrue(state().shouldAutoExpandDetails)
        assertTrue(state().copy(status = VirusTotalScanStatus.FAILED, report = null).shouldAutoExpandDetails)
    }

    @Test
    fun `community loading does not block installation after a strict scan`() {
        val completed = state(mode = VirusTotalScanMode.STRICT).copy(isCommunityLoading = true)
        assertFalse(completed.blocksInstallation)
        assertTrue(completed.copy(status = VirusTotalScanStatus.SCANNING).blocksInstallation)
    }
}
