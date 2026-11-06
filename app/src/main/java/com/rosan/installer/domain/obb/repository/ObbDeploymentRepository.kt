// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.obb.repository

import com.rosan.installer.domain.obb.model.ObbDeploymentResult

/**
 * Extracts OBB payloads from the sources of an install and copies them to
 * `Android/obb/<package>/`, trying Direct I/O, Shizuku and SAF in that order.
 */
interface ObbDeploymentRepository {
    suspend fun deploy(packageName: String, sourcePaths: List<String>): ObbDeploymentResult

    /** Whether a SAF tree has been granted for the final fallback tier. */
    fun hasSafTree(): Boolean
}
