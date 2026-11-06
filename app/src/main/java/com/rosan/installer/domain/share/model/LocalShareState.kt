// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.share.model

data class LocalShareConfig(
    val port: Int = LocalShareProtocol.DEFAULT_PORT,
    val pin: String = "",
) {
    val valid: Boolean
        get() = LocalShareProtocol.isValidPort(port) && LocalShareProtocol.isValidPin(pin)
}

data class LocalShareState(
    val running: Boolean = false,
    val config: LocalShareConfig = LocalShareConfig(),
    val addresses: List<String> = emptyList(),
    val receivedFiles: List<String> = emptyList(),
    val lastError: String? = null,
) {
    val primaryUrl: String?
        get() = addresses.firstOrNull()?.let { "http://$it:${config.port}" }
}
