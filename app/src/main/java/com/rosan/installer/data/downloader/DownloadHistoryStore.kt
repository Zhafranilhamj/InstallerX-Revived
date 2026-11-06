// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.downloader

import android.content.Context
import com.rosan.installer.domain.downloader.model.DownloadRecord
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** Small JSON-backed history; avoids a Room schema migration for this feature. */
class DownloadHistoryStore(
    context: Context,
    private val json: Json,
) {
    private val file = File(context.filesDir, "downloader/history.json")
    private val mutex = Mutex()

    suspend fun load(): List<DownloadRecord> = mutex.withLock { read() }

    suspend fun add(record: DownloadRecord) = mutex.withLock {
        write((read().filterNot { it.id == record.id } + record).takeLast(60))
    }

    suspend fun remove(id: String) = mutex.withLock {
        write(read().filterNot { it.id == id })
    }

    suspend fun clear() = mutex.withLock { write(emptyList()) }

    private fun read(): List<DownloadRecord> = runCatching {
        if (!file.isFile) return emptyList()
        json.decodeFromString<List<DownloadRecord>>(file.readText())
    }.getOrDefault(emptyList())

    private fun write(records: List<DownloadRecord>) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(records))
        }
    }
}
