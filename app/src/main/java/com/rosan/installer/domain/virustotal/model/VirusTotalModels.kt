// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.model

/**
 * How a file was submitted to VirusTotal.
 */
enum class VirusTotalScanMethod {
    /** Only the SHA-256 hash was sent to VirusTotal. */
    HASH,

    /** The file itself was uploaded to VirusTotal. */
    UPLOAD,
}

/**
 * The overall verdict for a scanned file.
 */
enum class VirusTotalVerdict {
    CLEAN,
    SUSPICIOUS,
    MALICIOUS,
    NOT_FOUND,
}

/**
 * A completed VirusTotal report for a single file.
 */
data class VirusTotalReport(
    val verdict: VirusTotalVerdict,
    val positives: Int = 0,
    val total: Int = 0,
    val detections: List<String> = emptyList(),
    val permalink: String? = null,
    val sha256: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val method: VirusTotalScanMethod,
    val analysisId: String? = null,
)

/**
 * The reason a VirusTotal request could not produce a report.
 */
enum class VirusTotalErrorKind {
    NO_API_KEY,
    INVALID_API_KEY,
    NETWORK_ERROR,
    FILE_TOO_LARGE,
    FILE_UNREADABLE,
    NOT_FOUND,
    QUOTA_EXCEEDED,
    TIMEOUT,
    UNKNOWN,
}

data class VirusTotalScanError(
    val kind: VirusTotalErrorKind,
    val message: String,
)

/**
 * The result of a scan attempt. Failures are values rather than exceptions so the UI can
 * distinguish "no report for this hash" from "the request failed".
 */
sealed interface VirusTotalScanOutcome {
    data class Success(val report: VirusTotalReport) : VirusTotalScanOutcome
    data class Failure(val error: VirusTotalScanError) : VirusTotalScanOutcome
}

/**
 * Thrown by the scanner implementation. Use cases translate this into a [VirusTotalScanError].
 */
class VirusTotalException(
    val kind: VirusTotalErrorKind,
    message: String,
) : Exception(message)

/**
 * VirusTotal API keys are 64 hexadecimal characters.
 */
object VirusTotalApiKeyFormat {
    private val API_KEY_PATTERN = Regex("^[a-fA-F0-9]{64}$")

    fun isValid(apiKey: String): Boolean = API_KEY_PATTERN.matches(apiKey.trim())
}
