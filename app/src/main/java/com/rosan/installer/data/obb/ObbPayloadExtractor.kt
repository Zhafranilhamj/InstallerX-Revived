// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.obb

import android.content.Context
import com.rosan.installer.domain.obb.model.ObbPathPolicy
import com.rosan.installer.domain.obb.model.ObbPayload
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Stages every OBB found in the install sources into private app storage.
 *
 * XAPK/APKM containers are opened as ZIP archives; standalone `.obb` files selected alongside
 * an APK are attached directly. Staging makes each payload reopenable so the writer can retry
 * with a different tier without keeping the source archive open.
 */
class ObbPayloadExtractor(private val context: Context) {
    suspend fun extract(packageName: String, sourcePaths: List<String>): List<ObbPayload> = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "obb-staging/${UUID.randomUUID()}").apply { mkdirs() }
        val payloads = mutableListOf<ObbPayload>()
        val seen = mutableSetOf<Pair<String, String>>()

        sourcePaths.distinct().forEach { path ->
            val source = File(path)
            if (!source.isFile) return@forEach
            when {
                source.extension.equals("obb", ignoreCase = true) -> {
                    val fileName = ObbPathPolicy.safeFileName(source.name) ?: return@forEach
                    val owner = ObbPathPolicy.packageNameFromFileName(fileName) ?: packageName
                    if (!ObbPathPolicy.isValidPackageName(owner)) return@forEach
                    if (seen.add(owner to fileName)) {
                        stage(source.inputStream(), owner, fileName, staging)?.let(payloads::add)
                    }
                }

                isContainer(source) -> extractContainer(source, packageName, staging, seen, payloads)
            }
        }
        payloads
    }

    private fun extractContainer(
        source: File,
        fallbackPackage: String,
        staging: File,
        seen: MutableSet<Pair<String, String>>,
        payloads: MutableList<ObbPayload>,
    ) {
        runCatching {
            ZipFile(source).use { archive ->
                archive.entries().asSequence()
                    .filter { !it.isDirectory && ObbPathPolicy.isObbFileName(it.name) }
                    .forEach { entry ->
                        if (entry.size > MAX_OBB_BYTES || payloads.sumOf { it.size } + entry.size > MAX_OBB_BYTES) {
                            return@forEach
                        }
                        val destination = ObbPathPolicy.destination(entry.name, fallbackPackage) ?: return@forEach
                        val (owner, fileName) = destination
                        if (!seen.add(owner to fileName)) return@forEach
                        val stream = runCatching { archive.getInputStream(entry) }.getOrNull() ?: return@forEach
                        stage(stream, owner, fileName, staging)?.let(payloads::add)
                    }
            }
        }
    }

    private fun stage(
        input: InputStream,
        packageName: String,
        fileName: String,
        staging: File,
    ): ObbPayload? = runCatching {
        val staged = File(staging, "$packageName.$fileName")
        input.use { source -> staged.outputStream().use { target -> source.copyTo(target) } }
        ObbPayload(
            fileName = fileName,
            packageName = packageName,
            size = staged.length(),
            stagedPath = staged.absolutePath,
        )
    }.onFailure { Timber.w(it, "Failed to stage OBB $fileName") }.getOrNull()

    private fun isContainer(file: File): Boolean {
        if (!CONTAINER_EXTENSIONS.contains(file.extension.lowercase())) return false
        return runCatching {
            file.inputStream().use { stream ->
                val header = ByteArray(4)
                stream.read(header) == 4 &&
                    header[0] == 0x50.toByte() &&
                    header[1] == 0x4B.toByte() &&
                    (header[2] == 3.toByte() || header[2] == 5.toByte() || header[2] == 7.toByte())
            }
        }.getOrDefault(false)
    }

    private companion object {
        const val MAX_OBB_BYTES = 4L * 1024 * 1024 * 1024
        val CONTAINER_EXTENSIONS = setOf("xapk", "apkm", "apks", "zip")
    }
}
