// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

/**
 * Validation for `splitName` values taken from `AndroidManifest.xml`.
 *
 * The platform uses the split name as a key inside the install session, and this app derives the
 * written entry name from it (`"$splitName.apk"`). An unvalidated value such as `../../shared_prefs/x`
 * therefore escaped the session naming scheme, so only plain identifier-like names are accepted.
 */
object SplitNamePolicy {
    /** Android split names are identifiers: letters, digits, `_`, `.` and `-`. */
    private val ALLOWED = Regex("^[A-Za-z0-9_.-]{1,64}$")

    fun isValid(splitName: String): Boolean {
        if (splitName == "." || splitName == "..") return false
        if (!ALLOWED.matches(splitName)) return false
        // A leading dot would create hidden files; a path separator is already excluded above.
        return !splitName.startsWith(".")
    }

    /** Same rule applied to the final session entry name (for example `base.apk`). */
    fun isValidEntryName(name: String): Boolean = name != "." && name != ".." && ALLOWED.matches(name)

    /** Returns a safe fallback name for an APK whose manifest carried an unusable split name. */
    fun sanitizeOrNull(splitName: String?): String? {
        val candidate = splitName?.trim().orEmpty()
        if (candidate.isEmpty()) return null
        return candidate.takeIf(::isValid)
    }
}
