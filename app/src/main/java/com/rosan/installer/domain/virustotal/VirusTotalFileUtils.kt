// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal

import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.File
import java.security.MessageDigest
import timber.log.Timber

/**
 * Calculates the SHA-256 hash of the top-level source of this entity.
 *
 * Hashing happens on the source the user actually selected (for example the .apks container),
 * not on a materialized install payload, so the hash matches what VirusTotal knows about the file.
 */
fun DataEntity.calculateSha256(): String? {
    val top = getSourceTop()
    return try {
        top.getInputStream()?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                digest.update(buffer, 0, bytesRead)
            }
            digest.digest().toHexString()
        }
    } catch (error: Exception) {
        Timber.e(error, "Failed to calculate SHA-256 for VirusTotal scan")
        null
    }
}

/**
 * A human-readable file name for the selected source.
 */
fun DataEntity.displayName(): String {
    val top = getSourceTop()
    return when (top) {
        is DataEntity.FileEntity -> File(top.path).name
        is DataEntity.ZipFileEntity -> top.name
        else -> top.toString()
    }.ifBlank { "upload.bin" }
}

private fun ByteArray.toHexString(): String {
    val builder = StringBuilder(size * 2)
    for (byte in this) {
        val value = byte.toInt() and 0xFF
        builder.append(HEX_DIGITS[value ushr 4])
        builder.append(HEX_DIGITS[value and 0x0F])
    }
    return builder.toString()
}

private const val HEX_DIGITS = "0123456789abcdef"
