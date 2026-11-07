// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C)2026 InstallerX Revived contributors
package com.rosan.installer.framework.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import timber.log.Timber

/**
 * Authenticated encryption for secrets that must live in DataStore (which is a plain XML file in the
 * app's private storage and shows up in adb backups, root file browsers and bug reports).
 *
 * The key is generated inside the Android Keystore and never leaves it. Values are stored as
 * `enc:v1:<base64(iv || ciphertext)>`; anything without that prefix is treated as a legacy
 * plaintext value so existing installs keep their saved key.
 */
object SecretCipher {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "installerx_secret_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "enc:v1:"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    /** Encrypts [plaintext]; returns it unchanged when the Keystore is unavailable. */
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty() || isEncrypted(plaintext)) return plaintext
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val payload = cipher.iv + ciphertext
            PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
        }.getOrElse { error ->
            // Never lose the value: writing plaintext is worse than crashing, so degrade loudly.
            Timber.w(error, "Could not encrypt the stored secret; keeping it in plaintext")
            plaintext
        }
    }

    /**
     * Decrypts a stored value. Plaintext legacy values are returned as-is. A value that claims to
     * be encrypted but cannot be decrypted (key invalidated, data corrupted) yields `null` so the
     * caller can ask the user to re-enter the secret instead of using garbage.
     */
    fun decrypt(stored: String): String? {
        if (stored.isEmpty()) return ""
        if (!isEncrypted(stored)) return stored
        return runCatching {
            val payload = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, IV_BYTES)
            val ciphertext = payload.copyOfRange(IV_BYTES, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrElse { error ->
            Timber.w(error, "Stored secret could not be decrypted")
            null
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
