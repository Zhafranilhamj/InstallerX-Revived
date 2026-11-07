// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.share

import com.rosan.installer.domain.share.model.LocalShareProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalShareProtocolTest {
    @Test
    fun `qr payload round trips through the endpoint parser`() {
        val payload = LocalShareProtocol.buildQrPayload("192.168.1.20:8765", "1234")
        assertTrue(payload.startsWith("installerx://tv?"))
        val endpoint = LocalShareProtocol.parseQrPayload(payload)
        assertEquals("http://192.168.1.20:8765", endpoint?.baseUrl)
        assertEquals("1234", endpoint?.pin)
        assertNull(LocalShareProtocol.parseQrPayload("https://example.com/tv"))
        assertNull(LocalShareProtocol.parseQrPayload("installerx://tv?u=ftp%3A%2F%2Fbad"))
    }

    @Test
    fun `pin security never accepts a wrong pin`() {
        assertTrue(LocalShareProtocol.matchesPin("1234", "1234"))
        assertFalse(LocalShareProtocol.matchesPin("1234", "9999"))
        assertFalse(LocalShareProtocol.matchesPin("1234", null))
        assertFalse(LocalShareProtocol.matchesPin("1234", ""))
        // The session cookie is no longer a PIN-derived value, so it must not authorize on its own.
        assertFalse(LocalShareProtocol.matchesPin("1234", "1234".padEnd(64, '0')))
        assertTrue(LocalShareProtocol.matchesPin("", null))
        assertFalse(LocalShareProtocol.isValidPin("12"))
        assertTrue(LocalShareProtocol.isValidPin(""))
    }

    @Test
    fun `generated pins are six digits and not identical`() {
        val pins = (1..200).map { LocalShareProtocol.generatePin() }
        assertTrue(pins.all { it.length == 6 && it.all { character -> character.isDigit() } })
        assertTrue(pins.toSet().size > 190, "SecureRandom PINs must not repeat")
    }

    @Test
    fun `only local network peers are accepted`() {
        listOf("127.0.0.1", "192.168.0.5", "10.0.0.2", "172.20.1.1", "169.254.1.2").forEach {
            assertTrue(LocalShareProtocol.isLocalPeer(it), it)
        }
        listOf("8.8.8.8", "1.1.1.1", "not-an-ip").forEach {
            assertFalse(LocalShareProtocol.isLocalPeer(it), it)
        }
    }

    @Test
    fun `base urls are normalized and ports validated`() {
        assertEquals("http://192.168.1.5:9000", LocalShareProtocol.normalizeBaseUrl("192.168.1.5:9000"))
        assertEquals("https://tv.local:8765", LocalShareProtocol.normalizeBaseUrl("https://tv.local"))
        assertNull(LocalShareProtocol.normalizeBaseUrl("ftp://tv.local"))
        assertFalse(LocalShareProtocol.isValidPort(80))
        assertTrue(LocalShareProtocol.isValidPort(8765))
    }
}
