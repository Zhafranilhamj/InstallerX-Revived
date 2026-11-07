// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import com.rosan.installer.data.appmanager.UninstallStatusHolder
import com.rosan.installer.domain.appmanager.model.UninstallState
import com.rosan.installer.domain.appmanager.model.UninstallStatus
import com.rosan.installer.framework.notification.UserActionPrompt
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/** Records the platform result of each batch uninstall request in the shared status log. */
class BatchUninstallReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val holder: UninstallStatusHolder by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(UninstallStatusHolder.EXTRA_PACKAGE_NAME) ?: return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // The platform is waiting for the user to confirm the uninstall. Recording FAILED here
            // reported a cancellation that never happened and left the package half-removed from
            // the caller's point of view.
            val confirmation = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
            if (confirmation == null) {
                Timber.w("Uninstall for $packageName needs user action but carried no intent")
                holder.add(
                    UninstallStatus(
                        packageName = packageName,
                        state = UninstallState.FAILED,
                        detail = detail.ifBlank { "status=$status, missing confirmation intent" },
                    ),
                )
                return
            }
            holder.add(UninstallStatus(packageName, UninstallState.PENDING))
            val shown = UserActionPrompt.launch(
                context = context,
                activity = null,
                actionIntent = confirmation,
                packageName = packageName,
                notificationId = notificationId(packageName),
            )
            if (!shown) {
                Timber.w("Could not surface the uninstall confirmation for $packageName")
                holder.add(
                    UninstallStatus(
                        packageName = packageName,
                        state = UninstallState.FAILED,
                        detail = "Could not show the confirmation dialog",
                    ),
                )
            }
            return
        }

        holder.add(
            UninstallStatus(
                packageName = packageName,
                state = if (status == PackageInstaller.STATUS_SUCCESS) UninstallState.SUCCESS else UninstallState.FAILED,
                detail = detail.ifBlank { "status=$status" },
            ),
        )
    }

    /** Stable per-package notification id so repeated prompts replace instead of piling up. */
    private fun notificationId(packageName: String): Int = 4300 + (packageName.hashCode() and 0xff)
}
