// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal

import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalUploadPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VirusTotalUploadPolicyTest {
    @Test
    fun `standard endpoint is inclusive up to 32 MB`() {
        for (size in listOf(0L, 1L, VirusTotalUploadPolicy.DIRECT_MAX_BYTES)) {
            assertFalse(VirusTotalUploadPolicy.requiresUploadUrl(size))
            assertNull(VirusTotalUploadPolicy.validationError(size))
        }
    }

    @Test
    fun `special endpoint starts immediately above 32 MB and includes 650 MB`() {
        for (size in listOf(VirusTotalUploadPolicy.DIRECT_MAX_BYTES + 1, 100L * 1024 * 1024, VirusTotalUploadPolicy.MAX_UPLOAD_BYTES)) {
            assertTrue(VirusTotalUploadPolicy.requiresUploadUrl(size))
            assertNull(VirusTotalUploadPolicy.validationError(size))
        }
    }

    @Test
    fun `rejects files above 650 MB and unknown sizes before upload`() {
        assertEquals(VirusTotalErrorKind.FILE_TOO_LARGE, VirusTotalUploadPolicy.validationError(VirusTotalUploadPolicy.MAX_UPLOAD_BYTES + 1))
        assertEquals(VirusTotalErrorKind.FILE_UNREADABLE, VirusTotalUploadPolicy.validationError(-1))
        assertFalse(VirusTotalUploadPolicy.requiresUploadUrl(VirusTotalUploadPolicy.MAX_UPLOAD_BYTES + 1))
    }
}
