// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.packageinfo.PackageIdentityStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.model.source.ZipEntryMetadataSource
import com.rosan.installer.domain.engine.provider.InstalledAppInfoProvider
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class PackagePreprocessor(
    private val installedAppInfoProvider: InstalledAppInfoProvider,
    // Kept in the constructor for Koin wiring compatibility; dedup no longer needs a zip provider
    // because content is verified with SHA-256 through the entity's own input stream.
    @Suppress("UNUSED_PARAMETER") unifiedZipFileProvider: UnifiedZipFileProvider,
) {

    data class ProcessedGroup(val packageName: String, val entities: List<AppEntity>, val installedInfo: InstalledAppInfo?)

    data class SessionTypeInfo(val isMultiAppSession: Boolean, val sessionType: DataType, val isFromSingleFile: Boolean)

    /**
     * Process raw data in parallel: Group -> Deduplicate -> Get installed system info.
     */
    suspend fun process(rawEntities: List<AppEntity>, includeSignature: Boolean = true): List<ProcessedGroup> = coroutineScope {
        rawEntities
            .groupBy { it.packageName }
            .map { (packageName, entities) ->
                async(Dispatchers.IO) {
                    // 1. Parallel Deduplication (IO intensive)
                    val deduplicated = deduplicateEntities(entities)
                    // 2. Parallel fetching of system info (IPC/IO intensive)
                    val installedInfo = installedAppInfoProvider.getByPackageName(packageName, includeSignature)

                    ProcessedGroup(packageName, deduplicated, installedInfo)
                }
            }.awaitAll()
    }

    /**
     * Determine session type (used for UI display logic).
     */
    fun determineSessionType(groups: List<ProcessedGroup>, rawEntities: List<AppEntity>): SessionTypeInfo {
        val allEntities = groups.flatMap { it.entities }

        // 1. Determine the number of source files
        val sourcePaths = allEntities.mapNotNull { it.data.sourcePath() }.distinct()
        val isFromSingleFile = sourcePaths.size == 1

        // 2. Get main container type
        val firstType = allEntities.firstOrNull()?.sourceType

        // 3. Determine if it is multi-app mode
        val isMultiPackage = groups.size > 1
        val hasMultipleBases = !isMultiPackage && allEntities.count { it is AppEntity.BaseEntity } > 1

        // Special logic: If it is a mixed module, force it not to be treated as multi-app mode to maintain integrity.
        val isMixedModule = firstType == DataType.MIXED_MODULE_APK || firstType == DataType.MIXED_MODULE_ZIP

        val isMultiAppSession = !isMixedModule && (isMultiPackage || hasMultipleBases)

        // 4. Correct the final container type display
        val finalContainerType = when {
            isMultiAppSession && firstType == DataType.MULTI_APK_ZIP -> DataType.MULTI_APK_ZIP
            isMultiAppSession -> DataType.MULTI_APK
            else -> firstType ?: DataType.APK
        }

        return SessionTypeInfo(isMultiAppSession, finalContainerType, isFromSingleFile)
    }

    // Internal: Deduplication logic
    private suspend fun deduplicateEntities(entities: List<AppEntity>): List<AppEntity> = coroutineScope {
        val bases = entities.filterIsInstance<AppEntity.BaseEntity>()
        // If there is only one Base, no deduplication calculation is needed, return directly
        if (bases.size <= 1) return@coroutineScope entities

        val cheapKeys = bases.map { it.cheapFingerprint() }
        // A (CRC32, size) pair can prove two entries are different, but never that they are the
        // same: CRC32 collisions are trivial to construct. Only the ambiguous candidates are hashed
        // with SHA-256, so the common case stays as fast as before without trusting the checksum.
        val ambiguousKeys = cheapKeys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        val digests = bases.mapIndexed { index, entity ->
            if (cheapKeys[index] in ambiguousKeys) {
                async(Dispatchers.IO) { entity.contentSha256() ?: cheapKeys[index] }
            } else {
                null
            }
        }.map { it?.await() }

        val seen = HashSet<String>()
        val distinctBases = bases.filterIndexed { index, _ ->
            val digest = digests[index]
            val key = if (digest != null) "sha256:$digest" else "crc:${cheapKeys[index]}"
            seen.add(key)
        }

        val others = entities.filter { it !is AppEntity.BaseEntity }
        distinctBases + others
    }

    /** SHA-256 of the decoded entry content, or `null` when the source cannot be read. */
    private fun AppEntity.BaseEntity.contentSha256(): String? = runCatching {
        data.getInputStreamWhileNotEmpty()?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }.getOrNull()

    /** Cheap identity used only to shortlist candidates for the SHA-256 check. */
    private fun AppEntity.BaseEntity.cheapFingerprint(): String {
        (data as? ZipEntryMetadataSource)?.zipEntryMetadata
            ?.takeIf { it.crc >= 0L && it.uncompressedSize >= 0L }
            ?.let { return "${it.crc}|${it.uncompressedSize}" }

        return when (val entityData = data) {
            is DataEntity.FileEntity -> "${entityData.path}|${entityData.getSize()}"
            is DataEntity.ZipFileEntity -> "${entityData.name}|${entityData.getSize()}"
            is DataEntity.SeekableZipEntryEntity -> "${entityData.crc}|${entityData.getSize()}"
            else -> "${packageName}_${versionCode}"
        }
    }

    /**
     * Checks if the incoming base APK is identical to the installed APK.
     * Only works for DataType.APK and assumes the data is a FileEntity.
     */
    suspend fun checkPackageIdentity(
        baseEntity: AppEntity.BaseEntity?,
        installedInfo: InstalledAppInfo?,
        sessionType: DataType,
    ): PackageIdentityStatus = coroutineScope {
        // 1. Fast applicability checks
        if (sessionType != DataType.APK) {
            return@coroutineScope PackageIdentityStatus.NOT_APPLICABLE
        }

        if (baseEntity == null || installedInfo == null) {
            return@coroutineScope PackageIdentityStatus.NOT_APPLICABLE
        }

        if (baseEntity.versionCode != installedInfo.versionCode ||
            baseEntity.versionName != installedInfo.versionName
        ) {
            return@coroutineScope PackageIdentityStatus.NOT_APPLICABLE
        }

        // 2. Validate installed APK path.
        val installedApkPath = installedInfo.sourceDir
        if (installedApkPath.isNullOrBlank()) {
            return@coroutineScope PackageIdentityStatus.ERROR
        }
        val installedApkFile = File(installedApkPath)
        if (!installedApkFile.exists() || !installedApkFile.isFile) {
            return@coroutineScope PackageIdentityStatus.ERROR
        }

        // 3. Extract the new APK file.
        // Safely cast to FileEntity as guaranteed by the caller constraints.
        val fileData = baseEntity.data as? DataEntity.FileEntity
            ?: return@coroutineScope PackageIdentityStatus.ERROR

        if (fileData is DataEntity.FileDescriptorEntity && !fileData.preInstallIdentityAnalysis) {
            return@coroutineScope PackageIdentityStatus.NOT_APPLICABLE
        }

        // 4. Fast-fail optimization: compare file sizes first.
        if (fileData.getSize() != installedApkFile.length()) {
            return@coroutineScope PackageIdentityStatus.DIFFERENT
        }

        // 5. Heavy validation: Full SHA-256 Hash Comparison.
        val newAppHashDeferred = async(Dispatchers.IO) { fileData.calculateSHA256() }
        val installedAppHashDeferred = async(Dispatchers.IO) { installedApkFile.calculateSHA256() }

        val newAppHash = newAppHashDeferred.await()
        val installedAppHash = installedAppHashDeferred.await()

        if (newAppHash != null && newAppHash == installedAppHash) {
            return@coroutineScope PackageIdentityStatus.IDENTICAL
        } else if (newAppHash != null && installedAppHash != null) {
            return@coroutineScope PackageIdentityStatus.DIFFERENT
        }

        return@coroutineScope PackageIdentityStatus.ERROR
    }
}
