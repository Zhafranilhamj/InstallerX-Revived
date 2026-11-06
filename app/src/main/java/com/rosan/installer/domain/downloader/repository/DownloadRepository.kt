// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.downloader.repository

import com.rosan.installer.domain.downloader.model.DownloadRecord
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    val history: StateFlow<List<DownloadRecord>>

    /** Reloads persisted history (call when the screen opens). */
    suspend fun refresh()

    /** Downloads one package, honouring the app's network-access setting. */
    suspend fun download(url: String, onProgress: (Float) -> Unit = {}): Result<DownloadRecord>

    suspend fun remove(id: String)

    suspend fun clear()
}
