// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.notification

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
        val forward = explicitTarget(actionIntent, context) ?: run {
            Timber.w("Refusing to post a confirmation notification for an intent without a system target")
            return false
        }
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            forward,
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
        // POST_NOTIFICATIONS is a runtime permission on API 33+; without it notify() is silently
        // dropped (and lint flags the unchecked call). Bail out instead of pretending it was shown.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return runCatching { manager.notify(notificationId, notification) }
            .onFailure { Timber.w(it, "Unable to post the user-action notification") }
            .isSuccess
    }

    /**
     * Rebuilds the platform-provided confirmation intent around an explicit target component.
     *
     * `PackageInstaller` hands us the intent for its own confirmation activity, but not every OEM
     * fills in a component. Forwarding such an implicit intent into a [PendingIntent] would let any
     * app that registers for the action receive it (CodeQL `android/implicit-pendingintent`), so the
     * intent is rebuilt with the resolved component pinned first and only the payload fields copied.
     * A resolver that is not part of the system image is rejected: the platform installer UI always
     * is, so this can only narrow the target, never hand it to a third party.
     *
     * @return an intent safe to forward, or `null` when no system component handles it.
     */
    private fun explicitTarget(actionIntent: Intent, context: Context): Intent? {
        val candidate = Intent(actionIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolvedComponent = candidate.component ?: run {
            val activityInfo = candidate.resolveActivityInfo(context.packageManager, 0) ?: return null
            val appFlags = activityInfo.applicationInfo?.flags ?: return null
            val isSystem = appFlags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                appFlags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
            if (!isSystem) return null
            ComponentName(activityInfo.packageName, activityInfo.name)
        }

        val forward = Intent().apply {
            component = resolvedComponent
            action = candidate.action
            if (candidate.data != null || candidate.type != null) {
                setDataAndType(candidate.data, candidate.type)
            }
            candidate.categories?.forEach { addCategory(it) }
            putExtras(candidate)
            clipData = candidate.clipData
            addFlags(candidate.flags)
        }
        return forward
    }
}
