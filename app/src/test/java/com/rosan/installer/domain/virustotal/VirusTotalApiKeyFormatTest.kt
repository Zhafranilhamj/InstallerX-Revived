// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal

import com.rosan.installer.domain.settings.model.preferences.VirusTotalScanMode
import com.rosan.installer.domain.virustotal.model.VirusTotalApiKeyFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VirusTotalApiKeyFormatTest {
    @Test
    fun `accepts a 64 character hexadecimal key`() {
        val key = "a".repeat(64)
        assertTrue(VirusTotalApiKeyFormat.isValid(key))
        assertTrue(VirusTotalApiKeyFormat.isValid(key.uppercase()))
        assertTrue(VirusTotalApiKeyFormat.isValid("  $key  "))
    }

    @Test
    fun `rejects malformed keys`() {
        val key = "a".repeat(64)
        assertFalse(VirusTotalApiKeyFormat.isValid(""))
        assertFalse(VirusTotalApiKeyFormat.isValid("  "))
        assertFalse(VirusTotalApiKeyFormat.isValid(key.drop(1)))
        assertFalse(VirusTotalApiKeyFormat.isValid(key + "a"))
        assertFalse(VirusTotalApiKeyFormat.isValid(key.replace('a', 'g')))
        assertFalse(VirusTotalApiKeyFormat.isValid(key.replace('a', '-')))
    }

    @Test
    fun `scan mode falls back to off`() {
        assertEquals(VirusTotalScanMode.OFF, VirusTotalScanMode.fromValueOrDefault(null))
        assertEquals(VirusTotalScanMode.OFF, VirusTotalScanMode.fromValueOrDefault("Unknown"))
        assertEquals(VirusTotalScanMode.NORMAL, VirusTotalScanMode.fromValueOrDefault("Normal"))
        assertEquals(VirusTotalScanMode.STRICT, VirusTotalScanMode.fromValueOrDefault("Strict"))
        assertEquals(VirusTotalScanMode.OFF, VirusTotalScanMode.fromValueOrDefault("Off"))
    }
}
