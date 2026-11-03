// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.obb.model

/** An OBB staged in the app's private cache, ready to be written to shared storage. */
data class ObbPayload(
    val fileName: String,
    val packageName: String,
    val size: Long,
    val stagedPath: String,
)

/** The write backend that succeeded (or was last attempted) for one payload. */
enum class ObbWriteTier {
    DIRECT,
    SHIZUKU,
    SAF,
}

data class ObbWriteResult(
    val payload: ObbPayload,
    val tier: ObbWriteTier? = null,
    val success: Boolean = false,
    val detail: String = "",
)

data class ObbDeploymentResult(
    val packageName: String,
    val results: List<ObbWriteResult> = emptyList(),
) {
    val hasPayloads: Boolean get() = results.isNotEmpty()

    val success: Boolean get() = hasPayloads && results.all { it.success }

    val tiers: Set<ObbWriteTier> get() = results.mapNotNull { it.tier }.toSet()

    fun summary(): String = if (!hasPayloads) {
        "No OBB files"
    } else {
        val written = results.count { it.success }
        "$written/${results.size} OBB files written" +
            tiers.takeIf { it.isNotEmpty() }?.joinToString(prefix = " (", postfix = ")") { it.name }.orEmpty()
    }
}
