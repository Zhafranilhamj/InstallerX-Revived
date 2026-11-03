// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.components

import com.rosan.installer.R
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VirusTotalWarningMappingTest {
    @Test
    fun `likely false positives keep the original warning and strict note resources`() {
        assertEquals(R.string.virustotal_false_positive_warning, VirusTotalVerdict.LIKELY_FALSE_POSITIVE.warningStringRes())
        assertEquals(R.string.virustotal_false_positive_strict_note, VirusTotalVerdict.LIKELY_FALSE_POSITIVE.strictNoteStringRes())
    }

    @Test
    fun `other verdicts keep their own safety messages`() {
        assertEquals(R.string.virustotal_scan_malicious_warning, VirusTotalVerdict.MALICIOUS.warningStringRes())
        assertEquals(R.string.virustotal_scan_suspicious_warning, VirusTotalVerdict.SUSPICIOUS.warningStringRes())
        assertNull(VirusTotalVerdict.MALICIOUS.strictNoteStringRes())
        assertNull(VirusTotalVerdict.CLEAN.warningStringRes())
        assertNull(VirusTotalVerdict.NOT_FOUND.warningStringRes())
    }
}
