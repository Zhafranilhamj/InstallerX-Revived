// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.util

import android.content.Context
import android.provider.MediaStore
import java.io.File
import timber.log.Timber

private const val DELETE_TAG = "DELETE_PATH"

fun deletePaths(context: Context, paths: List<String>) {
    // Allowlist: only delete inside app-private dirs, Downloads, cache, external files
    val allowedRoots = listOfNotNull(
        runCatching { context.filesDir.canonicalFile }.getOrNull(),
        runCatching { context.cacheDir.canonicalFile }.getOrNull(),
        runCatching { context.externalCacheDir?.canonicalFile }.getOrNull(),
        runCatching { context.getExternalFilesDir(null)?.canonicalFile }.getOrNull(),
        runCatching { File("/storage/emulated/0/Download").canonicalFile }.getOrNull(),
        runCatching { File("/sdcard/Download").canonicalFile }.getOrNull(),
    )
    for (path in paths) {
        val file = runCatching { File(path).canonicalFile }.getOrNull()
        if (file == null) {
            Timber.tag(DELETE_TAG).w("Invalid path skipped: $path")
            continue
        }

        Timber.tag(DELETE_TAG).d("Processing path for deletion: $path -> ${file.absolutePath}")

        // Path canonicalization and traversal check
        if (file.absolutePath == "/" || file.absolutePath == "/data" || file.absolutePath == "/system" || file.absolutePath == "/storage/emulated/0") {
            Timber.tag(DELETE_TAG).w("Blocked dangerous deletion of system root: ${file.absolutePath}")
            continue
        }
        val isAllowed = allowedRoots.any { root -> file.absolutePath == root.absolutePath || file.absolutePath.startsWith(root.absolutePath + File.separator) }
        if (!isAllowed) {
            Timber.tag(DELETE_TAG).w("Blocked deletion outside allowed roots: ${file.absolutePath} (allowed: ${allowedRoots.map { it.absolutePath }})")
            continue
        }
        // Symlink check: ensure canonical path is still within allowed roots
        if (file.canonicalPath != file.absolutePath) {
            val canonicalAllowed = allowedRoots.any { root -> file.canonicalPath.startsWith(root.canonicalPath + File.separator) || file.canonicalPath == root.canonicalPath }
            if (!canonicalAllowed) {
                Timber.tag(DELETE_TAG).w("Blocked symlink traversal deletion: $path -> ${file.canonicalPath}")
                continue
            }
        }

        try {
            if (file.exists()) {
                if (file.deleteRecursively()) {
                    Timber.tag(DELETE_TAG).d("Successfully deleted: $path")
                    removeFromMediaStore(context, file.absolutePath)
                } else {
                    Timber.tag(DELETE_TAG).w("Failed to delete: $path. Check for permissions or lock issues.")
                }
            } else {
                Timber.tag(DELETE_TAG).d("File/Directory does not exist, no action needed: $path")
            }
        } catch (e: SecurityException) {
            Timber.tag(DELETE_TAG).e(e, "SecurityException on deleting $path. Permission denied.")
        } catch (e: Exception) {
            Timber.tag(DELETE_TAG).e(e, "An unexpected error occurred while processing $path")
        }
    }
}

private fun removeFromMediaStore(context: Context, path: String) {
    try {
        val deletedRows = context.contentResolver.delete(
            MediaStore.Files.getContentUri("external"),
            "_data=?",
            arrayOf(path),
        )
        Timber.tag(DELETE_TAG).d("Removed $deletedRows MediaStore row(s) for: $path")
    } catch (e: SecurityException) {
        // The physical deletion has already succeeded. MediaStore cleanup is best effort because
        // it may be denied for a path owned by another provider or user on some Android builds.
        Timber.tag(DELETE_TAG).w(e, "Unable to remove deleted path from MediaStore: $path")
    } catch (e: Exception) {
        Timber.tag(DELETE_TAG).w(e, "MediaStore cleanup failed for: $path")
    }
}
