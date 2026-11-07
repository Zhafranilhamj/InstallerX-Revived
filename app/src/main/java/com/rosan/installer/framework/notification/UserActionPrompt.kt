// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.notification

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rosan.installer.R
import timber.log.Timber

/**
 * Surfaces a `PackageInstaller` confirmation activity that the platform reported through
 * `STATUS_PENDING_USER_ACTION`.
 *
 * Android 10+ blocks background activity starts, so starting the confirmation directly from a
 * background context throws `BackgroundActivityStartDenied` on many OEM builds. This helper prefers
 * the live [Activity] (if any), then a plain context start, and finally falls back to a high
 * priority notification carrying the very same intent, so the user can always complete the flow.
 */
object UserActionPrompt {
    private const val CHANNEL_ID = "user_action_confirmation"
    // Resource ids are not compile-time constants (non-final R), so these cannot be `const`.
    private val CHANNEL_NAME_RES = R.string.user_action_channel_name
    private val CHANNEL_DESC_RES = R.string.user_action_channel_desc

    /**
     * @return `true` when either the activity or the notification was shown.
     */
    fun launch(
        context: Context,
        activity: Activity?,
        actionIntent: Intent,
        packageName: String?,
        notificationId: Int,
    ): Boolean {
        val launchable = Intent(actionIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val started = runCatching {
            if (activity != null && !activity.isFinishing) {
                activity.startActivity(launchable)
            } else {
                context.startActivity(launchable)
            }
        }
        if (started.isSuccess) return true
        Timber.w(started.exceptionOrNull(), "Could not start the confirmation activity directly")
        return runCatching { notify(context, actionIntent, packageName, notificationId) }
            .onFailure { error -> Timber.w(error, "Could not post the confirmation notification") }
            .isSuccess
    }

    private fun notify(
        context: Context,
        actionIntent: Intent,
        packageName: String?,
        notificationId: Int,
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(CHANNEL_NAME_RES))
                .setDescription(context.getString(CHANNEL_DESC_RES))
                .build(),
        )
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(actionIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = packageName?.let { context.getString(R.string.user_action_notification_desc, it) }
            ?: context.getString(R.string.user_action_notification_desc_unknown)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(context.getString(R.string.user_action_notification_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(notificationId, notification)
        return true
    }
}
