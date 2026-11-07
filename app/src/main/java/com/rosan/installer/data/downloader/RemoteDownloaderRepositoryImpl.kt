// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.downloader

import android.content.Context
import com.rosan.installer.domain.downloader.model.DownloadRecord
import com.rosan.installer.domain.downloader.model.DownloadStatus
import com.rosan.installer.domain.downloader.model.DownloadUrlPolicy
import com.rosan.installer.domain.downloader.repository.DownloadRepository
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class RemoteDownloaderRepositoryImpl(
    context: Context,
    private val historyStore: DownloadHistoryStore,
    private val appSettingsRepository: AppSettingsRepository,
) : DownloadRepository {
    private val directory = File(context.filesDir, "downloader/packages")
    private val client = OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .readTimeout(java.time.Duration.ofSeconds(30))
        .writeTimeout(java.time.Duration.ofSeconds(30))
        .build()
    private val _history = MutableStateFlow<List<DownloadRecord>>(emptyList())
    override val history: StateFlow<List<DownloadRecord>> = _history.asStateFlow()

    init {
        directory.mkdirs()
    }

    override suspend fun download(url: String, onProgress: (Float) -> Unit): Result<DownloadRecord> = withContext(Dispatchers.IO) {
        if (!DownloadUrlPolicy.isSupported(url)) {
            return@withContext Result.failure(IllegalArgumentException("Only http and https URLs are supported"))
        }
        if (!appSettingsRepository.preferencesFlow.first().allowInternetAccess) {
            return@withContext Result.failure(IllegalStateException("Network access is disabled in settings"))
        }
        refreshHistory()
        val id = UUID.randomUUID().toString()
        val fileName = DownloadUrlPolicy.suggestedFileName(url)
        val target = File(directory, "$id.$fileName")
        val temporary = File(directory, "$id.part")
        val result = runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body
                val total = body.contentLength()
                body.byteStream().use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            written += count
                            if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                            if (written > MAX_DOWNLOAD_BYTES) error("Download exceeds the 2 GB limit")
                        }
                        if (written == 0L) error("Empty response")
                    }
                }
                if (!temporary.renameTo(target)) error("Could not save the downloaded package")
                DownloadRecord(
                    id = id,
                    url = url,
                    fileName = fileName,
                    size = target.length(),
                    status = DownloadStatus.COMPLETED,
                    timestamp = System.currentTimeMillis(),
                    localPath = target.absolutePath,
                )
            }
        }
        val record = result.getOrElse { error ->
            Timber.w(error, "Remote download failed for $url")
            temporary.delete()
            DownloadRecord(
                id = id,
                url = url,
                fileName = fileName,
                status = DownloadStatus.FAILED,
                timestamp = System.currentTimeMillis(),
                error = error.message,
            )
        }
        historyStore.add(record)
        refreshHistory()
        // Prune old files on disk past 60-item limit
        pruneExcessFiles()
        result
    }

    private suspend fun pruneExcessFiles() = withContext(Dispatchers.IO) {
        val all = historyStore.load().sortedByDescending { it.timestamp }
        if (all.size > 60) {
            all.drop(60).forEach { old -> runCatching { File(old.localPath).delete() } }
        }
        // Also clean orphaned .part files
        directory.listFiles()?.filter { it.name.endsWith(".part") }?.forEach { part ->
            if (System.currentTimeMillis() - part.lastModified() > 24 * 60 * 60 * 1000) runCatching { part.delete() }
        }
    }

    override suspend fun refresh() = refreshHistory()

    override suspend fun remove(id: String) {
        _history.value.firstOrNull { it.id == id }?.localPath?.let { runCatching { File(it).delete() } }
        historyStore.remove(id)
        refreshHistory()
    }

    override suspend fun clear() {
        _history.value.forEach { record -> runCatching { File(record.localPath).delete() } }
        historyStore.clear()
        refreshHistory()
    }

    private suspend fun refreshHistory() {
        _history.value = historyStore.load().sortedByDescending { it.timestamp }
    }

    private companion object {
        const val MAX_DOWNLOAD_BYTES = 2L * 1024 * 1024 * 1024
    }
}
