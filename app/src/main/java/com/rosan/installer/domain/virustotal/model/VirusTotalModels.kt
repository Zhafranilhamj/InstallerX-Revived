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

    /**
     * Engines flagged the file, but the pattern matches the detections that are routinely false
     * positives for repackaged, optimised, or regionally signed APKs.
     */
    LIKELY_FALSE_POSITIVE,
    SUSPICIOUS,
    MALICIOUS,
    NOT_FOUND,
    ;

    /** True when at least one engine flagged the file. */
    val hasDetections: Boolean
        get() = this == LIKELY_FALSE_POSITIVE || this == SUSPICIOUS || this == MALICIOUS
}

/**
 * One engine detection, including the name the engine reported (for example
 * `Trojan.AndroidOS.Boogr.gsh`).
 */
data class VirusTotalDetection(
    val engine: String,
    val name: String? = null,
)

/**
 * Heuristics used to decide whether a set of detections looks like the noise VirusTotal commonly
 * returns for otherwise harmless APKs.
 *
 * A file is only treated as a likely false positive when very few engines flagged it (both in
 * absolute terms and as a share of the engines that produced a result), or when every flagging
 * engine only reported a generic/ML heuristic name. Anything broader would weaken the real
 * malicious verdicts that Strict mode blocks on.
 */
object VirusTotalFalsePositiveHeuristics {
    /** Maximum number of flagging engines that can still be considered low confidence. */
    const val MAX_FLAGGING_ENGINES = 5

    /** Maximum share (in percent) of flagging engines that can still be considered low confidence. */
    const val MAX_FLAGGING_PERCENT = 10

    /**
     * Detection families that describe behaviour which is almost never a false positive. When any
     * engine reports one of these, the report keeps its malicious severity even if only one engine
     * flagged the file.
     */
    private val SEVERE_NAME_MARKERS = listOf(
        "banker",
        "banking",
        "spy",
        "stealer",
        "credential",
        "ransom",
        "backdoor",
        "keylog",
        "rat.",
        "sms",
        "dropper",
        "downloader",
        "root",
        "exploit",
    )

    private val GENERIC_NAME_MARKERS = listOf(
        "generic",
        "heur",
        "ml.",
        "mlatt",
        "gen.",
        "artemis",
        "packed",
        "packer",
        "riskware",
        "pua",
        "unwanted",
        "suspicious",
        "possible",
        "detected",
        "boogr",
        "apkprotector",
        "hiddad",
        "agent",
    )

    fun isLikelyFalsePositive(
        positives: Int,
        total: Int,
        detections: List<VirusTotalDetection>,
    ): Boolean {
        if (positives <= 0) return false
        if (positives > MAX_FLAGGING_ENGINES) return false

        // A concrete malware family stays malicious no matter how few engines reported it.
        if (detections.any { isSevereName(it.name) }) return false

        val percent = if (total > 0) (positives * 100) / total else MAX_FLAGGING_PERCENT
        if (percent <= MAX_FLAGGING_PERCENT) return true

        // Some engines report several different generic names for the same file. Only fall back to
        // the name check when every detected engine used a generic heuristic signature.
        return detections.isNotEmpty() && detections.all { isGenericHeuristicName(it.name) }
    }

    /** A detection name that describes a concrete, high-risk malware family. */
    fun isSevereName(result: String?): Boolean {
        val normalized = result?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
        return SEVERE_NAME_MARKERS.any { normalized.contains(it) }
    }

    fun isGenericHeuristicName(result: String?): Boolean {
        val normalized = result?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
        return GENERIC_NAME_MARKERS.any { normalized.contains(it) }
    }
}

/**
 * A completed VirusTotal report for a single file.
 */
data class VirusTotalReport(
    val verdict: VirusTotalVerdict,
    val positives: Int = 0,
    val total: Int = 0,
    val detections: List<VirusTotalDetection> = emptyList(),
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
