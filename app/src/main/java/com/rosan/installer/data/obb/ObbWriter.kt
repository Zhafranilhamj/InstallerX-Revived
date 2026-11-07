// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.obb

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.obb.model.ObbPathPolicy
import com.rosan.installer.domain.obb.model.ObbPayload
import com.rosan.installer.domain.obb.model.ObbWriteResult
import com.rosan.installer.domain.obb.model.ObbWriteTier
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.framework.privileged.core.execution.dispatcher.useUserService
import java.io.File
import rikka.shizuku.Shizuku
import timber.log.Timber

/**
 * Multi-tier OBB writer: direct filesystem I/O, then the Shizuku/Root privileged user service,
 * then a user-granted SAF tree. Every tier writes atomically through a temporary file.
 */
class ObbWriter(
    private val context: Context,
    private val safTreeStore: ObbSafTreeStore,
    private val capabilityProvider: DeviceCapabilityProvider,
) {
    fun write(payload: ObbPayload): ObbWriteResult {
        var lastDetail = ""
        for (tier in ObbWriteTier.entries) {
            val result = runCatching {
                when (tier) {
                    ObbWriteTier.DIRECT -> writeDirect(payload)
                    ObbWriteTier.SHIZUKU -> writePrivileged(payload)
                    ObbWriteTier.SAF -> writeSaf(payload)
                }
            }.getOrElse { error ->
                Timber.w(error, "OBB tier ${tier.name} failed for ${payload.fileName}")
                error.message.orEmpty().ifBlank { error::class.java.simpleName }
            }
            if (result == null) return ObbWriteResult(payload, tier, true)
            lastDetail = result
        }
        return ObbWriteResult(payload, ObbWriteTier.SAF, false, lastDetail)
    }

    /** Returns `null` on success, otherwise a human-readable reason. */
    private fun writeDirect(payload: ObbPayload): String? {
        val root = File(Environment.getExternalStorageDirectory(), ObbPathPolicy.OBB_ROOT)
        val directory = File(root, payload.packageName)
        if (!directory.isDirectory && !directory.mkdirs()) return "Direct I/O: cannot create ${directory.path}"
        val target = File(directory, payload.fileName)
        val temporary = File(directory, "${payload.fileName}.tmp")
        val stagedFile = File(payload.stagedPath)
        val originalExists = target.exists()
        val originalBackup = if (originalExists) File(directory, "${payload.fileName}.bak") else null
        return try {
            // Preserve existing OBB until new one verified
            if (originalExists) runCatching { target.copyTo(originalBackup!!, overwrite = true) }
            stagedFile.inputStream().use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            if (temporary.length() != stagedFile.length()) {
                temporary.delete()
                if (originalBackup?.exists() == true) originalBackup.delete()
                return "Direct I/O: staged size mismatch"
            }
            if (temporary.renameTo(target) || (target.delete() && temporary.renameTo(target))) {
                originalBackup?.delete()
                null
            } else {
                temporary.delete()
                // restore backup
                if (originalBackup?.exists() == true) runCatching { originalBackup.renameTo(target) }
                "Direct I/O: cannot replace ${target.name}"
            }
        } catch (error: Exception) {
            temporary.delete()
            if (originalBackup?.exists() == true) runCatching { originalBackup.renameTo(target) }
            "Direct I/O: ${error.message.orEmpty()}"
        } finally {
            originalBackup?.delete()
        }
    }

    /** Sends the staged file descriptor to the existing privileged user service. */
    private fun writePrivileged(payload: ObbPayload): String? {
        if (!shizukuReady()) return "Shizuku is not available or not granted"
        val descriptor = ParcelFileDescriptor.open(
            File(payload.stagedPath),
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
        var written = false
        return try {
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = Authorizer.Shizuku,
            ) { service ->
                written = service.privileged.writeObb(descriptor, payload.packageName, payload.fileName)
            }
            if (written) null else "Shizuku: privileged OBB write was refused"
        } finally {
            runCatching { descriptor.close() }
        }
    }

    private fun shizukuReady(): Boolean = runCatching {
        capabilityProvider.shizukuAuthorizedFlow.value &&
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /** Uses DocumentsContract directly so no extra support dependency is needed. */
    private fun writeSaf(payload: ObbPayload): String? {
        val tree = safTreeStore.get() ?: return "SAF: no Android/obb folder has been granted"
        val resolver = context.contentResolver
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val obbRoot = resolveObbRoot(resolver, root)
            ?: return "SAF: cannot create Android/obb in the granted folder"
        val directory = findOrCreateDirectory(resolver, obbRoot, payload.packageName)
            ?: return "SAF: cannot create ${payload.packageName} directory"
        val existing = findChild(resolver, directory, payload.fileName)
        if (existing != null) {
            runCatching { DocumentsContract.deleteDocument(resolver, existing) }
        }
        val document = DocumentsContract.createDocument(
            resolver,
            directory,
            "application/octet-stream",
            payload.fileName,
        ) ?: return "SAF: cannot create ${payload.fileName}"
        return try {
            resolver.openOutputStream(document, "w")?.use { output ->
                File(payload.stagedPath).inputStream().use { input -> input.copyTo(output) }
            } ?: return "SAF: cannot open output stream"
            null
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
            "SAF: ${error.message.orEmpty()}"
        }
    }

    private fun resolveObbRoot(resolver: ContentResolver, treeRoot: Uri): Uri? {
        val displayName = queryDisplayName(resolver, treeRoot)
        if (displayName.equals("obb", ignoreCase = true)) return treeRoot
        val android = findOrCreateDirectory(resolver, treeRoot, "Android") ?: return null
        return findOrCreateDirectory(resolver, android, "obb")
    }

    private fun findOrCreateDirectory(resolver: ContentResolver, parent: Uri, name: String): Uri? {
        findChild(resolver, parent, name)?.let { return it }
        return runCatching {
            DocumentsContract.createDocument(resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
        }.getOrNull()
    }

    private fun findChild(resolver: ContentResolver, parent: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            parent,
            DocumentsContract.getDocumentId(parent),
        )
        return runCatching {
            resolver.query(
                children,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    if (nameColumn >= 0 && cursor.getString(nameColumn) == name && idColumn >= 0) {
                        return DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(idColumn))
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun queryDisplayName(resolver: ContentResolver, document: Uri): String? = runCatching {
        resolver.query(
            document,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val column = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        }
    }.getOrNull()
}
