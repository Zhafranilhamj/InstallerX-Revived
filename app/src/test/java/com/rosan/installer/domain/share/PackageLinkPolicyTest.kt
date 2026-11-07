// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.share.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageLinkPolicyTest {
    @Test
    fun `accepts package containers over http and https`() {
        listOf(
            "https://example.com/app.apk",
            "https://example.com/app.apks",
            "https://example.com/app.apkm",
            "https://example.com/app.xapk",
            "https://example.com/app.zip",
            "http://192.168.1.10:8080/files/app.apk",
            "https://example.com/download/App.APK",
        ).forEach { url ->
            assertTrue(PackageLinkPolicy.isAllowedPackageUrl(url), url)
        }
    }

    @Test
    fun `accepts container links that carry a query string`() {
        listOf(
            "https://example.com/app.apk?token=abc123&expires=99",
            "https://example.com/dl/app.apks#fragment",
            "https://example.com/app.apk?redirect=https%3A%2F%2Fevil.example%2Fx.exe",
        ).forEach { url ->
            assertTrue(PackageLinkPolicy.isAllowedPackageUrl(url), url)
        }
    }

    @Test
    fun `rejects links whose extension only appears in the query`() {
        // The extension has to be in the path: `.../get?file=app.apk` can serve anything.
        listOf(
            "https://example.com/get?file=app.apk",
            "https://example.com/download?name=app.xapk",
        ).forEach { url ->
            assertFalse(PackageLinkPolicy.isAllowedPackageUrl(url), url)
        }
    }

    @Test
    fun `rejects non-network schemes`() {
        listOf(
            "javascript:alert(1)",
            "file:///sdcard/app.apk",
            "content://com.example.provider/app.apk",
            "data:application/vnd.android.package-archive;base64,AAAA",
            "intent://evil#Intent;scheme=http;end",
            "ftp://example.com/app.apk",
        ).forEach { url ->
            assertFalse(PackageLinkPolicy.isAllowedPackageUrl(url), url)
        }
    }

    @Test
    fun `rejects urls that carry credentials`() {
        listOf(
            "https://user:pass@example.com/app.apk",
            "https://user@example.com/app.apk",
            "http://admin:@192.168.0.2/app.apk",
        ).forEach { url ->
            assertFalse(PackageLinkPolicy.isAllowedPackageUrl(url), url)
        }
    }

    @Test
    fun `rejects loopback hosts and non-package paths`() {
        listOf(
            "http://localhost/app.apk",
            "http://127.0.0.1:8765/app.apk",
            "http://[::1]/app.apk",
            "https://example.com/app.exe",
            "https://example.com/download",
            "https://example.com/",
            "not a url",
            "",
        ).forEach { url ->
            assertFalse(PackageLinkPolicy.isAllowedPackageUrl(url), url)
        }
    }
}
