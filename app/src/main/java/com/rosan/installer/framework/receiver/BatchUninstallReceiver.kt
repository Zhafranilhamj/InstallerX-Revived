// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.rosan.installer.data.appmanager.UninstallStatusHolder
import com.rosan.installer.domain.appmanager.model.UninstallState
import com.rosan.installer.domain.appmanager.model.UninstallStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Records the platform result of each batch uninstall request in the shared status log. */
class BatchUninstallReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val holder: UninstallStatusHolder by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(UninstallStatusHolder.EXTRA_PACKAGE_NAME) ?: return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        holder.add(
            UninstallStatus(
                packageName = packageName,
                state = if (status == PackageInstaller.STATUS_SUCCESS) UninstallState.SUCCESS else UninstallState.FAILED,
                detail = detail.ifBlank { "status=$status" },
            ),
        )
    }
}
