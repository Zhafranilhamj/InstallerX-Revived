// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import androidx.core.content.IntentCompat
import com.rosan.installer.data.engine.executor.appinstaller.LocalIntentReceiver
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.exception.UninstallException
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.engine.model.error.UninstallErrorType
import com.rosan.installer.framework.notification.UserActionPrompt
import timber.log.Timber

object PackageManagerUtil {
    /**
     * The platform can ask for user interaction at most once per install; anything beyond that is
     * a stuck confirmation, not a reason to keep waiting. A bounded loop replaces the previous
     * unbounded recursion, which never terminated when no activity could be started.
     */
    private const val MAX_USER_ACTION_ATTEMPTS = 2

    /**
     * @param activity optional foreground activity; used so the confirmation is not blocked by the
     * Android 10+ background activity start restriction. Background callers pass `null` and the
     * prompt degrades to a notification.
     */
    suspend fun installResultVerify(
        context: Context,
        receiver: LocalIntentReceiver,
        activity: Activity? = null,
    ) {
        var userActionAttempts = 0
        while (true) {
            val intent = receiver.getResult()
            val status =
                intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val action =
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)

            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION && action != null) {
                userActionAttempts++
                if (userActionAttempts > MAX_USER_ACTION_ATTEMPTS) {
                    Timber.w("Install confirmation could not be surfaced after $MAX_USER_ACTION_ATTEMPTS attempts")
                    throw InstallException(
                        InstallErrorType.UNKNOWN,
                        "Install Failure $status [user action did not resolve]",
                    )
                }
                UserActionPrompt.launch(
                    context = context,
                    activity = activity,
                    actionIntent = action,
                    packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME),
                    notificationId = NOTIFICATION_ID_INSTALL,
                )
                continue
            }

            if (status == PackageInstaller.STATUS_SUCCESS) return

            val legacyStatus = intent.getIntExtra(
                PackageInstallerHidden.EXTRA_LEGACY_STATUS,
                PackageInstaller.STATUS_FAILURE,
            )
            val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            val ecpMsg = "Install Failure $status#$legacyStatus [$msg]"

            // Resolve error type dynamically and throw unified exception
            val errorType = InstallErrorType.fromLegacyCode(legacyStatus)
            throw InstallException(errorType, ecpMsg)
        }
    }

    suspend fun uninstallResultVerify(
        context: Context,
        receiver: LocalIntentReceiver,
        activity: Activity? = null,
    ) {
        var userActionAttempts = 0
        while (true) {
            val intent = receiver.getResult()
            val status =
                intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val action =
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)

            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION && action != null) {
                userActionAttempts++
                if (userActionAttempts > MAX_USER_ACTION_ATTEMPTS) {
                    Timber.w("Uninstall confirmation could not be surfaced after $MAX_USER_ACTION_ATTEMPTS attempts")
                    throw UninstallException(
                        UninstallErrorType.UNKNOWN,
                        "Uninstall Failure $status [user action did not resolve]",
                    )
                }
                UserActionPrompt.launch(
                    context = context,
                    activity = activity,
                    actionIntent = action,
                    packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME),
                    notificationId = NOTIFICATION_ID_UNINSTALL,
                )
                continue
            }

            if (status == PackageInstaller.STATUS_SUCCESS) return

            val legacyStatus = intent.getIntExtra(PackageInstallerHidden.EXTRA_LEGACY_STATUS, 0)
            val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            val ecpMsg = "Uninstall Failure $status#$legacyStatus [$msg]"

            // Resolve error type dynamically and throw unified exception
            val errorType = UninstallErrorType.fromLegacyCode(legacyStatus)
            throw UninstallException(errorType, ecpMsg)
        }
    }

    private const val NOTIFICATION_ID_INSTALL = 4201
    private const val NOTIFICATION_ID_UNINSTALL = 4202
}
