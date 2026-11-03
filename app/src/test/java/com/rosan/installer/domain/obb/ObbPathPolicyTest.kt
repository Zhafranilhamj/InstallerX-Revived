// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.obb

import com.rosan.installer.domain.obb.model.ObbPathPolicy
import com.rosan.installer.domain.obb.model.ObbWriteTier
import com.rosan.installer.domain.obb.model.ObbDeploymentResult
import com.rosan.installer.domain.obb.model.ObbPayload
import com.rosan.installer.domain.obb.model.ObbWriteResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObbPathPolicyTest {
    @Test
    fun `xapk entries map to Android obb package folder`() {
        assertEquals(
            "com.example.game" to "main.42.com.example.game.obb",
            ObbPathPolicy.destination("Android/obb/com.example.game/main.42.com.example.game.obb", null),
        )
        assertEquals(
            "com.example.game" to "patch.42.com.example.game.obb",
            ObbPathPolicy.destination("com.example.game/patch.42.com.example.game.obb", null),
        )
        assertEquals(
            "Android/obb/com.example.game/main.42.com.example.game.obb",
            ObbPathPolicy.relativePath("com.example.game", "main.42.com.example.game.obb"),
        )
    }

    @Test
    fun `bare obb name and standalone obb use the apk package as fallback`() {
        assertEquals(
            "com.rosan.installer" to "main.7.some.obb",
            ObbPathPolicy.destination("main.7.some.obb", "com.rosan.installer"),
        )
        assertEquals("com.example", ObbPathPolicy.packageNameFromFileName("main.9.com.example.obb"))
        assertNull(ObbPathPolicy.packageNameFromFileName("random.obb"))
    }

    @Test
    fun `path traversal and non obb names are rejected`() {
        assertNull(ObbPathPolicy.destination("../../evil.obb", "com.example"))
        assertNull(ObbPathPolicy.destination("/absolute/main.1.com.example.obb", "com.example"))
        assertNull(ObbPathPolicy.destination("notes.txt", "com.example"))
        assertNull(ObbPathPolicy.destination("random.obb", "not-a-package"))
        assertFalse(ObbPathPolicy.isValidPackageName("com..example"))
    }

    @Test
    fun `deployment result summarizes tiers and failures`() {
        val payload = ObbPayload("main.1.com.example.obb", "com.example", 10L, "/tmp/staged")
        val result = ObbDeploymentResult(
            "com.example",
            listOf(
                ObbWriteResult(payload, ObbWriteTier.DIRECT, true),
                ObbWriteResult(payload, ObbWriteTier.SAF, false, "denied"),
            ),
        )
        assertFalse(result.success)
        assertEquals(setOf(ObbWriteTier.DIRECT, ObbWriteTier.SAF), result.tiers)
        assertTrue(result.summary().contains("1/2"))
        assertTrue(ObbDeploymentResult("com.example").summary().contains("No OBB"))
    }
}
