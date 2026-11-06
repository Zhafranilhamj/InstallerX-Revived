// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.appmanager

import com.rosan.installer.domain.appmanager.model.UninstallStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Shared log between the batch-uninstall request and the broadcast result receiver. */
class UninstallStatusHolder {
    private val _log = MutableStateFlow<List<UninstallStatus>>(emptyList())
    val log: StateFlow<List<UninstallStatus>> = _log.asStateFlow()

    fun add(entry: UninstallStatus) {
        _log.update { current ->
            (current.filterNot { it.packageName == entry.packageName } + entry).takeLast(100)
        }
    }

    fun clear() {
        _log.value = emptyList()
    }

    companion object {
        const val ACTION_UNINSTALL_RESULT = "com.rosan.installer.action.UNINSTALL_RESULT"
        const val EXTRA_PACKAGE_NAME = "com.rosan.installer.extra.PACKAGE_NAME"
    }
}
