// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.obb

import com.rosan.installer.domain.obb.model.ObbDeploymentResult
import com.rosan.installer.domain.obb.repository.ObbDeploymentRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ObbDeploymentRepositoryImpl(
    private val extractor: ObbPayloadExtractor,
    private val writer: ObbWriter,
    private val safTreeStore: ObbSafTreeStore,
) : ObbDeploymentRepository {
    override suspend fun deploy(packageName: String, sourcePaths: List<String>): ObbDeploymentResult = withContext(Dispatchers.IO) {
        val payloads = extractor.extract(packageName, sourcePaths)
            .filter { it.packageName == packageName }
        if (payloads.isEmpty()) return@withContext ObbDeploymentResult(packageName)

        val results = payloads.map { payload ->
            writer.write(payload).also { outcome ->
                Timber.i("OBB ${payload.fileName}: tier=${outcome.tier}, success=${outcome.success}, detail=${outcome.detail}")
            }
        }
        payloads.forEach { runCatching { File(it.stagedPath).delete() } }
        ObbDeploymentResult(packageName, results)
    }

    override fun hasSafTree(): Boolean = safTreeStore.get() != null
}
