// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.util.timber

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A Timber Tree that logs to files asynchronously.
 * Uses a buffered Channel to prevent blocking the UI thread and OOM issues.
 */
@Suppress("LogNotTimber")
class FileLoggingTree(private val context: Context) : Timber.DebugTree() {

    companion object {
        private const val TAG = "FileLoggingTree"
        const val LOG_DIR_NAME = "logs"
        const val LOG_SUFFIX = ".log"
        private const val MAX_LOG_FILES = 2
        private const val MAX_FILE_SIZE = 4 * 1024 * 1024L // 4MB
        private const val MAX_LOG_AGE_MS = 24 * 60 * 60 * 1000L // 24 Hours
        private const val CHANNEL_CAPACITY = 1000
    }

    private val logDir: File by lazy { File(context.cacheDir, LOG_DIR_NAME) }

    private val entryDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileNameDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    // Drop oldest logs if the consumer can't keep up with the producer.
    private val logChannel = Channel<LogEntry>(
        capacity = CHANNEL_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentLogFile: File? = null

    init {
        scope.launch {
            ensureDirectoryExists()
            initializeFromExistingFile()

            logChannel.consumeEach { entry ->
                writeToFile(formatEntry(entry))
            }
        }
    }

    // Override to ensure the tag formatting matches the logic in App.kt
    override fun createStackElementTag(element: StackTraceElement): String? = super.createStackElementTag(element)
        ?.substringBefore('$')
        ?.take(23)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Filter out low priority logs in production if needed, generally INFO/WARN/ERROR
        // For debugging builds, keeping DEBUG/VERBOSE is fine.

        // Capture the event time, but leave date formatting, stack traces, and string building
        // to the IO consumer. Startup logging should not compete with the first Compose frame.
        logChannel.trySend(LogEntry(System.currentTimeMillis(), priority, tag ?: "Unknown", message, t))
    }

    private fun formatEntry(entry: LogEntry): String = buildString {
        append(getCurrentTimestamp(entry.timestampMillis))
        append(' ')
        append(priorityToString(entry.priority))
        append('/')
        append(entry.tag)
        append(": ")
        append(entry.message)
        append('\n')
        entry.throwable?.let { append(Log.getStackTraceString(it)).append('\n') }
    }

    private data class LogEntry(
        val timestampMillis: Long,
        val priority: Int,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )

    private fun ensureDirectoryExists() {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }

    private fun writeToFile(content: String) {
        try {
            ensureLogFileReady()
            currentLogFile?.appendText(content)
        } catch (e: Exception) {
            // Only log critical IO failures to Logcat
            Log.e(TAG, "Failed to write log to file", e)
        }
    }

    private fun ensureLogFileReady() {
        val now = System.currentTimeMillis()
        val file = currentLogFile
        var needNewFile = false

        if (!logDir.exists()) {
            logDir.mkdirs()
            needNewFile = true
        }

        if (file == null || !file.exists()) {
            needNewFile = true
        } else {
            if (file.length() > MAX_FILE_SIZE || now - file.lastModified() > MAX_LOG_AGE_MS) {
                needNewFile = true
            }
        }

        if (needNewFile) {
            rotateLogs()
        }
    }

    private fun initializeFromExistingFile() {
        try {
            val lastFile = logDir.listFiles { _, name -> name.endsWith(LOG_SUFFIX) }
                ?.maxByOrNull { it.lastModified() }

            if (lastFile != null && lastFile.exists()) {
                val now = System.currentTimeMillis()
                // Resume writing if file is fresh and small enough
                if (now - lastFile.lastModified() < MAX_LOG_AGE_MS && lastFile.length() < MAX_FILE_SIZE) {
                    currentLogFile = lastFile
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init from existing file", e)
        }
    }

    private fun rotateLogs() {
        createNewLogFile()
        cleanOldLogFiles()
    }

    private fun createNewLogFile() {
        try {
            val fileName = fileNameDateFormat.format(Date()) + LOG_SUFFIX
            val newFile = File(logDir, fileName)

            // Handle naming collision rare edge case
            currentLogFile = if (newFile.exists()) {
                val uniqueName = "${fileNameDateFormat.format(Date())}_${System.currentTimeMillis()}$LOG_SUFFIX"
                File(logDir, uniqueName)
            } else {
                newFile
            }

            currentLogFile?.createNewFile()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create new log file", e)
        }
    }

    private fun cleanOldLogFiles() {
        try {
            val files = logDir.listFiles { _, name -> name.endsWith(LOG_SUFFIX) } ?: return
            if (files.size > MAX_LOG_FILES) {
                files.sortByDescending { it.lastModified() }
                // Keep the newest N files, delete the rest
                for (i in MAX_LOG_FILES until files.size) {
                    files[i].delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old logs", e)
        }
    }

    private fun getCurrentTimestamp(timestampMillis: Long): String = synchronized(entryDateFormat) {
        entryDateFormat.format(Date(timestampMillis))
    }

    private fun priorityToString(priority: Int) = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }

    fun release() {
        scope.cancel()
        logChannel.close()
    }
}
