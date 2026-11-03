// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import android.content.Context
import com.rosan.installer.domain.share.model.LocalShareConfig
import com.rosan.installer.domain.share.model.LocalShareProtocol

class LocalShareConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): LocalShareConfig = LocalShareConfig(
        port = preferences.getInt(KEY_PORT, LocalShareProtocol.DEFAULT_PORT).takeIf(LocalShareProtocol::isValidPort)
            ?: LocalShareProtocol.DEFAULT_PORT,
        pin = preferences.getString(KEY_PIN, "").orEmpty().takeIf(LocalShareProtocol::isValidPin) ?: "",
    )

    fun save(config: LocalShareConfig) {
        preferences.edit()
            .putInt(KEY_PORT, config.port)
            .putString(KEY_PIN, config.pin)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "local_share"
        const val KEY_PORT = "port"
        const val KEY_PIN = "pin"
    }
}
