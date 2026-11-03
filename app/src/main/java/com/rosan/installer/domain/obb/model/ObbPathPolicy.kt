// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.obb.model

/**
 * Pure path rules shared by archive extraction and the tiered writer.
 *
 * OBB archives (XAPK/APKM) use several layouts: `Android/obb/<pkg>/<file>`, `<pkg>/<file>`, or
 * a bare `main.<version>.<pkg>.obb`. Standalone OBB files are passed as their own file name.
 */
object ObbPathPolicy {
    const val OBB_ROOT = "Android/obb"

    private val PACKAGE_REGEX = Regex("""^[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)+$""")
    private val OBB_NAME_REGEX = Regex("""^(?:main|patch)\.\d+\.(.+)\.obb$""", RegexOption.IGNORE_CASE)

    fun isObbFileName(name: String): Boolean = name.endsWith(".obb", ignoreCase = true)

    fun isValidPackageName(name: String): Boolean = PACKAGE_REGEX.matches(name)

    /** `main.42.com.example.game.obb` -> `com.example.game`. */
    fun packageNameFromFileName(fileName: String): String? =
        OBB_NAME_REGEX.matchEntire(fileName)?.groupValues?.get(1)?.takeIf(::isValidPackageName)

    /** Rejects traversal, absolute paths and anything that is not a plain file name. */
    fun safeFileName(fileName: String): String? {
        if (fileName.contains("..") || fileName.startsWith('/') || fileName.contains('\\')) return null
        val candidate = fileName.substringAfterLast('/')
        if (candidate.isEmpty() || candidate == "." || candidate == "..") return null
        if (candidate.contains('\u0000') || !isObbFileName(candidate)) return null
        if (!candidate.matches(Regex("""^[A-Za-z0-9._-]+\.obb$""", RegexOption.IGNORE_CASE))) return null
        return candidate
    }

    private fun packageFromEntryPath(entryName: String): String? {
        val normalized = entryName.replace('\\', '/')
        val parts = normalized.split('/').filter { it.isNotEmpty() }
        val marker = parts.indexOfFirst { it.equals("obb", ignoreCase = true) }
        val candidate = if (marker >= 0 && marker + 1 < parts.size) parts[marker + 1] else null
        return candidate?.takeIf(::isValidPackageName)
    }

    /**
     * Maps an archive entry (or standalone file name) to its destination package and file name.
     *
     * @param fallbackPackage package of the APK being installed; used when the OBB name itself
     * does not carry a package name.
     */
    fun destination(entryName: String, fallbackPackage: String?): Pair<String, String>? {
        val fileName = safeFileName(entryName) ?: return null
        val packageName = packageNameFromFileName(fileName)
            ?: packageFromEntryPath(entryName)
            ?: fallbackPackage?.takeIf(::isValidPackageName)
            ?: return null
        return packageName to fileName
    }

    fun relativePath(packageName: String, fileName: String): String = "$OBB_ROOT/$packageName/$fileName"
}
