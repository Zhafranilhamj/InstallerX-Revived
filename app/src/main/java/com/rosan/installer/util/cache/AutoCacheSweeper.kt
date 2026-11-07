// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.util.cache

import android.content.Context
import com.rosan.installer.util.timber.FileLoggingTree.Companion.LOG_DIR_NAME
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AutoCacheSweeper(private val context: Context) {
    // Only sweep files older than 48 hours; never run eagerly on process start for active sessions.
    private val maxCacheAgeMs = 48L * 60 * 60 * 1000

    init {
        sweep()
    }

    private fun sweep() {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("Starting automatic cache sweep via Koin...")
            try {
                val now = System.currentTimeMillis()
                val cutoff = now - maxCacheAgeMs
                val cacheRoots = listOfNotNull(
                    context.cacheDir,
                    context.externalCacheDir,
                )
                // Also sweep unbounded filesDir growth
                val filesRoots = listOf(
                    File(context.filesDir, "downloader/packages"),
                    File(context.filesDir, "local_share"),
                ).filter { it.exists() }

                val blacklistExtensions = listOf(".lck", ".lock")
                val blacklistNames = listOf(LOG_DIR_NAME)
                // Preserve active session and staging directories
                val protectedDirNames = setOf("installer_sessions", "downloader", "local_share", "tv_share", "logs")

                fun shouldSkipByName(file: File): Boolean {
                    if (blacklistNames.contains(file.name)) return true
                    if (blacklistExtensions.any { file.name.endsWith(it) }) return true
                    return false
                }

                fun clearFileSafe(file: File, isRoot: Boolean = false): Boolean {
                    if (!file.exists()) return true
                    if (shouldSkipByName(file)) return false
                    // Never delete protected staging dirs at root level; sweep contents instead
                    if (isRoot && protectedDirNames.contains(file.name)) {
                        // For protected dirs, only sweep children older than cutoff
                        val children = file.listFiles() ?: return false
                        var clearedCount = 0
                        children.forEach { child ->
                            if (child.lastModified() < cutoff && clearFileSafe(child)) clearedCount++
                        }
                        return false
                    }
                    if (file.lastModified() >= cutoff) {
                        // Preserve recent/active files (downloads, session caches)
                        return false
                    }
                    if (file.isDirectory) {
                        val children = file.listFiles()
                        var allChildrenDeleted = true
                        children?.forEach { child ->
                            if (!clearFileSafe(child)) {
                                allChildrenDeleted = false
                            }
                        }
                        if (!allChildrenDeleted) return false
                    }
                    return file.delete()
                }

                var totalCleared = 0
                cacheRoots.forEach { root ->
                    root.listFiles()?.forEach { child ->
                        if (clearFileSafe(child, isRoot = true)) totalCleared++
                    }
                }
                // Sweep filesDir with same age policy but limit to old files only
                filesRoots.forEach { root ->
                    root.listFiles()?.forEach { child ->
                        if (child.lastModified() < cutoff && clearFileSafe(child)) totalCleared++
                    }
                    // Remove empty staging dirs older than cutoff
                    if (root.exists() && root.listFiles()?.isEmpty() == true && root.lastModified() < cutoff) {
                        runCatching { root.delete() }
                    }
                }
                Timber.d("Automatic cache sweep completed. Cleared items: $totalCleared")
            } catch (e: Exception) {
                Timber.e(e, "Error occurred during automatic cache sweep")
            }
        }
    }
}
