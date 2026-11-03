// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.rosan.installer.R
import com.rosan.installer.data.share.LocalShareManager
import com.rosan.installer.domain.share.model.LocalShareConfig
import com.rosan.installer.domain.share.model.LocalShareProtocol
import com.rosan.installer.ui.activity.SettingsActivity
import org.koin.android.ext.android.inject
import timber.log.Timber

/** Keeps the local HTTP server alive while the dashboard is shared on the local network. */
class LocalShareService : Service() {
    private val manager: LocalShareManager by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        promoteToForeground()

        if (intent?.action == ACTION_STOP || intent == null) {
            manager.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val port = intent.getIntExtra(EXTRA_PORT, LocalShareProtocol.DEFAULT_PORT)
        val pin = intent.getStringExtra(EXTRA_PIN).orEmpty()
        val result = manager.start(LocalShareConfig(port, pin))
        if (result.isFailure) {
            Timber.w(result.exceptionOrNull(), "Local sharing server did not start")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        manager.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW,
        ).setName(getString(R.string.local_share_channel_name))
            .setDescription(getString(R.string.local_share_channel_desc))
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun promoteToForeground() {
        val stopIntent = Intent(this, LocalShareService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openPending = PendingIntent.getActivity(
            this,
            2,
            Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(getString(R.string.local_share_notification_title))
            .setContentText(getString(R.string.local_share_notification_desc))
            .setContentIntent(openPending)
            .addAction(0, getString(R.string.close), stopPending)
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val NOTIFICATION_ID = 1007
        private const val CHANNEL_ID = "local_share_channel"
        const val ACTION_START = "com.rosan.installer.action.LOCAL_SHARE_START"
        const val ACTION_STOP = "com.rosan.installer.action.LOCAL_SHARE_STOP"
        const val EXTRA_PORT = "local_share_port"
        const val EXTRA_PIN = "local_share_pin"

        fun start(context: Context, config: LocalShareConfig) {
            val intent = Intent(context, LocalShareService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, config.port)
                putExtra(EXTRA_PIN, config.pin)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocalShareService::class.java).apply { action = ACTION_STOP }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
