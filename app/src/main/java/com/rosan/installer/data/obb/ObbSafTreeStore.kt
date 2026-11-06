// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.obb

import android.content.Context
import android.net.Uri

/** Persists the user-granted SAF tree used as the last OBB write fallback. */
class ObbSafTreeStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun get(): Uri? = preferences.getString(KEY_TREE_URI, null)
        ?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun set(uri: Uri?) {
        preferences.edit().apply {
            if (uri == null) remove(KEY_TREE_URI) else putString(KEY_TREE_URI, uri.toString())
        }.apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "obb_saf"
        const val KEY_TREE_URI = "tree_uri"
    }
}
