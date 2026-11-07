// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalShareSessionStoreTest {
    @Test
    fun `issued tokens are accepted and unknown tokens are not`() {
        val store = LocalShareSessionStore()

        val token = store.issue()

        assertTrue(store.isValid(token))
        assertFalse(store.isValid("deadbeef"))
        assertFalse(store.isValid(null))
        assertFalse(store.isValid(""))
    }

    @Test
    fun `tokens are random 128 bit hex and never repeat`() {
        val store = LocalShareSessionStore()

        val tokens = (1..100).map { store.issue() }

        assertEquals(100, tokens.toSet().size)
        assertTrue(tokens.all { it.length == 32 && it.all { character -> character in '0'..'9' || character in 'a'..'f' } })
    }

    @Test
    fun `sessions expire after the configured ttl`() {
        var now = 1_000L
        val store = LocalShareSessionStore(ttlMillis = 500L, clock = { now })

        val token = store.issue()
        assertTrue(store.isValid(token))

        now += 499L
        assertTrue(store.isValid(token))

        now += 2L
        assertFalse(store.isValid(token))
    }

    @Test
    fun `the store is bounded so a flood cannot exhaust memory`() {
        val store = LocalShareSessionStore(maxSessions = 4)

        val tokens = (1..10).map { store.issue() }

        // The oldest entries are evicted; the most recent ones stay valid.
        assertFalse(store.isValid(tokens.first()))
        assertTrue(store.isValid(tokens.last()))
    }

    @Test
    fun `clearing revokes every session`() {
        val store = LocalShareSessionStore()
        val token = store.issue()

        store.clear()

        assertFalse(store.isValid(token))
    }
}
