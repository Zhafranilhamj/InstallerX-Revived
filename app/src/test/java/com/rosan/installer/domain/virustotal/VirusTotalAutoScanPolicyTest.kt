// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal

import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.automaticScanMethod
import com.rosan.installer.domain.virustotal.model.isMandatoryScan
import com.rosan.installer.ui.page.main.installer.VirusTotalScanState
import com.rosan.installer.ui.page.main.installer.VirusTotalScanStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class VirusTotalAutoScanPolicyTest {
    @Test
    fun `normal mode starts a hash lookup without uploading`() {
        assertEquals(VirusTotalScanMethod.HASH, VirusTotalScanMode.NORMAL.automaticScanMethod())
        assertFalse(VirusTotalScanMode.NORMAL.isMandatoryScan())
    }

    @Test
    fun `strict mode keeps the mandatory upload gate`() {
        assertEquals(VirusTotalScanMethod.UPLOAD, VirusTotalScanMode.STRICT.automaticScanMethod())
        assertTrue(VirusTotalScanMode.STRICT.isMandatoryScan())
    }

    @Test
    fun `off mode starts no scan`() {
        assertNull(VirusTotalScanMode.OFF.automaticScanMethod())
        assertFalse(VirusTotalScanMode.OFF.isMandatoryScan())
    }

    @Test
    fun `an automatic normal scan never blocks installation`() {
        val scanning = VirusTotalScanState(
            mode = VirusTotalScanMode.NORMAL,
            status = VirusTotalScanStatus.SCANNING,
            method = VirusTotalScanMethod.HASH,
        )
        assertFalse(scanning.blocksInstallation)
        assertFalse(scanning.isBlockedByMalware)
        assertFalse(scanning.isStrict)
    }
}
