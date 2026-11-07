// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.auth

import android.content.Context
import android.content.Intent
import com.rosan.installer.domain.engine.exception.AuthenticationFailedException
import com.rosan.installer.framework.activity.BiometricsAuthenticationActivity
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Safely initiates biometric authentication (non-throwing version).
 */
suspend fun Context.safeBiometricAuth(title: String, subTitle: String, sessionId: String = "global", timeoutMs: Long = 60_000): Boolean {
    val channel = BiometricAuthBridge.channelFor(sessionId)
    // Drain any stale value
    channel.tryReceive()

    val intent = Intent(this, BiometricsAuthenticationActivity::class.java).apply {
        putExtra("auth_title", title)
        putExtra("auth_subtitle", subTitle)
        putExtra("auth_session_id", sessionId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    BiometricAuthBridge.isAuthenticating = true
    try {
        this.startActivity(intent)
    } catch (e: Exception) {
        BiometricAuthBridge.isAuthenticating = false
        BiometricAuthBridge.removeChannel(sessionId)
        throw e
    }

    return try {
        withTimeoutOrNull(timeoutMs) {
            channel.receive()
        } ?: false.also { BiometricAuthBridge.removeChannel(sessionId) }
    } finally {
        BiometricAuthBridge.isAuthenticating = false
        BiometricAuthBridge.removeChannel(sessionId)
    }
}

/**
 * Safely initiates biometric authentication (throws exception on failure).
 */
suspend fun Context.safeBiometricAuthOrThrow(title: String, subTitle: String) {
    val success = safeBiometricAuth(title, subTitle)
    if (!success) {
        throw AuthenticationFailedException("Biometric authentication failed or was cancelled.")
    }
}
