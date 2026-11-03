// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.downloader

import com.rosan.installer.domain.downloader.model.DownloadUrlPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadUrlPolicyTest {
    @Test
    fun `only http and https urls without credentials are accepted`() {
        assertTrue(DownloadUrlPolicy.isSupported("https://example.com/app.apk"))
        assertTrue(DownloadUrlPolicy.isSupported("http://192.168.1.5:8765/app.xapk"))
        assertFalse(DownloadUrlPolicy.isSupported("file:///sdcard/app.apk"))
        assertFalse(DownloadUrlPolicy.isSupported("https://user:pass@example.com/app.apk"))
        assertFalse(DownloadUrlPolicy.isSupported("not a url"))
    }

    @Test
    fun `package extensions are recognised and file names are sanitized`() {
        assertTrue(DownloadUrlPolicy.looksLikePackage("https://example.com/a/app.APK?dl=1"))
        assertTrue(DownloadUrlPolicy.looksLikePackage("https://example.com/game.xapk"))
        assertFalse(DownloadUrlPolicy.looksLikePackage("https://example.com/index.html"))
        assertEquals("app.apk", DownloadUrlPolicy.suggestedFileName("https://example.com/a/app.apk?dl=1"))
        assertEquals("downloaded.apk", DownloadUrlPolicy.suggestedFileName("https://example.com/"))
    }
}
