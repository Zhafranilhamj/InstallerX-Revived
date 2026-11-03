// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.appmanager

import com.rosan.installer.domain.appmanager.model.AppListFilter
import com.rosan.installer.domain.appmanager.model.AppListQuery
import com.rosan.installer.domain.appmanager.model.AppSort
import com.rosan.installer.domain.appmanager.model.InstalledAppDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppListQueryTest {
    private val apps = listOf(
        InstalledAppDetails("com.zebra", "Zebra", apkSize = 10, installTime = 100, lastUsedTime = 5),
        InstalledAppDetails("com.alpha", "Alpha", apkSize = 90, installTime = 300, lastUsedTime = 1),
        InstalledAppDetails("android.system", "System UI", apkSize = 50, installTime = 200, isSystem = true, isEnabled = false),
    )

    @Test
    fun `system apps are hidden by default and search matches label and package`() {
        val visible = AppListQuery.apply(apps, AppListFilter())
        assertEquals(listOf("com.alpha", "com.zebra"), visible.map { it.packageName })
        assertEquals(3, AppListQuery.apply(apps, AppListFilter(includeSystem = true)).size)
        assertEquals(1, AppListQuery.apply(apps, AppListFilter(query = "zebra", includeSystem = true)).size)
        assertEquals(1, AppListQuery.apply(apps, AppListFilter(query = "android.system", includeSystem = true)).size)
    }

    @Test
    fun `sorting works by size install date last used and reverses`() {
        assertEquals(
            listOf("com.zebra", "com.alpha"),
            AppListQuery.apply(apps, AppListFilter(sort = AppSort.SIZE)).map { it.packageName },
        )
        assertEquals(
            listOf("com.alpha", "com.zebra"),
            AppListQuery.apply(apps, AppListFilter(sort = AppSort.SIZE, descending = true)).map { it.packageName },
        )
        assertEquals(
            listOf("com.zebra", "com.alpha"),
            AppListQuery.apply(apps, AppListFilter(sort = AppSort.INSTALL_DATE)).map { it.packageName },
        )
        assertEquals(
            listOf("com.alpha", "com.zebra"),
            AppListQuery.apply(apps, AppListFilter(sort = AppSort.LAST_USED)).map { it.packageName },
        )
        assertTrue(AppListQuery.apply(emptyList(), AppListFilter()).isEmpty())
    }
}
