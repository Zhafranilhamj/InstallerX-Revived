// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.activity

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rosan.installer.framework.auth.BiometricAuthBridge
import timber.log.Timber

class BiometricsAuthenticationActivity : FragmentActivity() {
    private var isResultSent = false
    private var isChangingConfig = false
    private val sessionId: String by lazy { intent.getStringExtra("auth_session_id") ?: "global" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("auth_title") ?: ""
        val subTitle = intent.getStringExtra("auth_subtitle") ?: ""

        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subTitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    sendResultAndFinish(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e("Biometric auth error ($errorCode): $errString")
                    // Don't send false on rotation-triggered destroy; only on real auth error
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED || !isChangingConfigurations) {
                        sendResultAndFinish(false)
                    }
                }
            },
        )

        biometricPrompt.authenticate(promptInfo)
    }

    private fun sendResultAndFinish(success: Boolean) {
        if (!isResultSent) {
            isResultSent = true
            // Prefer per-session channel, fallback to legacy
            runCatching { BiometricAuthBridge.channelFor(sessionId).trySend(success) }
            runCatching { BiometricAuthBridge.authResultChannel.trySend(success) }
        }
        finish()
    }

    override fun onDestroy() {
        // Rotation triggers onDestroy with isChangingConfigurations=true; don't send false-negative
        if (!isChangingConfigurations && !isResultSent) {
            sendResultAndFinish(false)
        }
        super.onDestroy()
    }
}
