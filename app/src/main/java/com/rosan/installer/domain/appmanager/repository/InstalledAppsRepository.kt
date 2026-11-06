// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.appmanager.repository

import com.rosan.installer.domain.appmanager.model.InstalledAppDetails
import com.rosan.installer.domain.appmanager.model.UninstallStatus
import kotlinx.coroutines.flow.StateFlow

interface InstalledAppsRepository {
    /** Detailed snapshot of installed user and system packages. */
    suspend fun list(): List<InstalledAppDetails>

    /** Live batch-uninstall log shared with the broadcast receiver that receives results. */
    val uninstallLog: StateFlow<List<UninstallStatus>>

    /** Requests platform uninstall for each package and records the outcome in [uninstallLog]. */
    fun uninstall(packageNames: List<String>)

    fun clearLog()
}
