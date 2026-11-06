// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.appmanager

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.rosan.installer.domain.appmanager.model.InstalledAppDetails
import com.rosan.installer.domain.appmanager.model.UninstallState
import com.rosan.installer.domain.appmanager.model.UninstallStatus
import com.rosan.installer.domain.appmanager.repository.InstalledAppsRepository
import com.rosan.installer.framework.receiver.BatchUninstallReceiver
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

class InstalledAppsRepositoryImpl(
    private val context: Context,
    private val holder: UninstallStatusHolder,
) : InstalledAppsRepository {
    override val uninstallLog: StateFlow<List<UninstallStatus>> get() = holder.log

    override suspend fun list(): List<InstalledAppDetails> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0
        val packages = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }
        }.getOrDefault(emptyList())
        val lastUsed = lastUsedTimes()
        packages.mapNotNull { info -> runCatching { toDetails(packageManager, info, lastUsed) }.getOrNull() }
    }

    private fun toDetails(
        packageManager: PackageManager,
        info: PackageInfo,
        lastUsed: Map<String, Long>,
    ): InstalledAppDetails {
        val application = info.applicationInfo
        val label = application?.loadLabel(packageManager)?.toString().orEmpty().ifBlank { info.packageName }
        val size = buildList {
            application?.sourceDir?.let(::add)
            application?.splitSourceDirs?.let { addAll(it) }
        }.sumOf { path -> runCatching { File(path).length() }.getOrDefault(0L) }
        return InstalledAppDetails(
            packageName = info.packageName,
            label = label,
            versionName = info.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            },
            minSdk = application?.minSdkVersion ?: 0,
            targetSdk = application?.targetSdkVersion ?: 0,
            installTime = info.firstInstallTime,
            updateTime = info.lastUpdateTime,
            lastUsedTime = lastUsed[info.packageName] ?: 0L,
            apkSize = size,
            permissions = info.requestedPermissions?.toList().orEmpty(),
            signatureSha256 = signingCertificates(info).map(::sha256),
            isSystem = application?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false,
            isEnabled = application?.enabled ?: true,
            installerPackageName = runCatching {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(info.packageName)
            }.getOrNull(),
        )
    }

    @Suppress("DEPRECATION")
    private fun signingCertificates(info: PackageInfo): Array<out Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.signingInfo?.apkContentsSigners.orEmpty()
    } else {
        info.signatures.orEmpty()
    }

    private fun sha256(signature: Signature): String = MessageDigest.getInstance("SHA-256")
        .digest(signature.toByteArray())
        .joinToString("") { "%02x".format(it) }

    /** Usage access is optional; without it "last used" stays zero instead of being fabricated. */
    private fun lastUsedTimes(): Map<String, Long> = runCatching {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
        val now = System.currentTimeMillis()
        manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - THIRTY_DAYS_MS, now)
            .orEmpty()
            .associate { it.packageName to it.lastTimeUsed }
    }.getOrElse { error ->
        Timber.d(error, "Usage stats are unavailable")
        emptyMap()
    }

    override fun uninstall(packageNames: List<String>) {
        val installer = context.packageManager.packageInstaller
        packageNames.distinct().forEach { packageName ->
            holder.add(UninstallStatus(packageName, UninstallState.PENDING))
            val intent = Intent(context, BatchUninstallReceiver::class.java).apply {
                action = UninstallStatusHolder.ACTION_UNINSTALL_RESULT
                putExtra(UninstallStatusHolder.EXTRA_PACKAGE_NAME, packageName)
            }
            val pending = android.app.PendingIntent.getBroadcast(
                context,
                packageName.hashCode() and 0x7fffffff,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
            runCatching { installer.uninstall(packageName, pending.intentSender) }
                .onFailure { error ->
                    holder.add(
                        UninstallStatus(
                            packageName = packageName,
                            state = UninstallState.FAILED,
                            detail = error.message.orEmpty(),
                        ),
                    )
                }
        }
    }

    override fun clearLog() = holder.clear()

    private companion object {
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }
}
