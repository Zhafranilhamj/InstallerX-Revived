// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.virustotal.model.VirusTotalReport

/**
 * Thin abstraction over the VirusTotal API v3.
 *
 * Implementations must not throw for expected API failures such as an invalid key, a missing
 * report, or a rate limit. They should throw [com.rosan.installer.domain.virustotal.model.VirusTotalException]
 * with the appropriate kind instead.
 */
interface VirusTotalScanner {
    /**
     * Checks whether the supplied API key is accepted by VirusTotal.
     */
    suspend fun validateApiKey(apiKey: String): Result<Unit>

    /**
     * Looks up an existing report by SHA-256 hash.
     */
    suspend fun lookupByHash(apiKey: String, sha256: String): VirusTotalReport

    /**
     * Uploads a file and waits for the analysis to complete.
     */
    suspend fun uploadAndScan(
        apiKey: String,
        data: DataEntity,
        fileName: String,
    ): VirusTotalReport
}
