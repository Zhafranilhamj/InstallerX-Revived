// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.appmanager.model

data class InstalledAppDetails(
    val packageName: String,
    val label: String,
    val versionName: String? = null,
    val versionCode: Long = 0,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
    val installTime: Long = 0,
    val updateTime: Long = 0,
    val lastUsedTime: Long = 0,
    val apkSize: Long = 0,
    val permissions: List<String> = emptyList(),
    val signatureSha256: List<String> = emptyList(),
    val isSystem: Boolean = false,
    val isEnabled: Boolean = true,
    val installerPackageName: String? = null,
)

enum class AppSort {
    NAME,
    INSTALL_DATE,
    UPDATE_DATE,
    SIZE,
    LAST_USED,
}

data class AppListFilter(
    val query: String = "",
    val includeUser: Boolean = true,
    val includeSystem: Boolean = false,
    val sort: AppSort = AppSort.NAME,
    val descending: Boolean = false,
)

/** Pure list logic so sorting/filtering can be verified without an Android framework. */
object AppListQuery {
    fun apply(items: List<InstalledAppDetails>, filter: AppListFilter): List<InstalledAppDetails> {
        val query = filter.query.trim()
        val filtered = items.filter { app ->
            val typeAllowed = if (app.isSystem) filter.includeSystem else filter.includeUser
            val matches = query.isEmpty() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            typeAllowed && matches
        }
        val comparator = when (filter.sort) {
            AppSort.NAME -> compareBy<InstalledAppDetails> { it.label.lowercase() }.thenBy { it.packageName }
            AppSort.INSTALL_DATE -> compareBy<InstalledAppDetails> { it.installTime }.thenBy { it.label.lowercase() }
            AppSort.UPDATE_DATE -> compareBy<InstalledAppDetails> { it.updateTime }.thenBy { it.label.lowercase() }
            AppSort.SIZE -> compareBy<InstalledAppDetails> { it.apkSize }.thenBy { it.label.lowercase() }
            AppSort.LAST_USED -> compareBy<InstalledAppDetails> { it.lastUsedTime }.thenBy { it.label.lowercase() }
        }
        val sorted = filtered.sortedWith(comparator)
        return if (filter.descending) sorted.asReversed() else sorted
    }
}
