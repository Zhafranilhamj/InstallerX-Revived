// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.auth

import kotlinx.coroutines.channels.Channel

/**
 * Cross-component communication bridge. Completely replaces dangerous static callback variables.
 */
object BiometricAuthBridge {
    // Per-session Rendezvous channels to avoid cross-session contamination
    private val channels = mutableMapOf<String, Channel<Boolean>>()
    private val lock = Any()

    fun channelFor(sessionId: String): Channel<Boolean> = synchronized(lock) {
        channels.getOrPut(sessionId) { Channel(Channel.RENDEZVOUS) }
    }

    fun removeChannel(sessionId: String) = synchronized(lock) {
        channels.remove(sessionId)?.close()
    }

    // Legacy global channel kept for compatibility but deprecated; use channelFor
    @Deprecated("Use channelFor(sessionId)")
    val authResultChannel = Channel<Boolean>(Channel.CONFLATED)

    @Volatile
    var isAuthenticating = false
}
