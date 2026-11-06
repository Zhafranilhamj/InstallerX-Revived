// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.appmanager.model

enum class UninstallState { PENDING, SUCCESS, FAILED }

data class UninstallStatus(
    val packageName: String,
    val state: UninstallState = UninstallState.PENDING,
    val detail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
