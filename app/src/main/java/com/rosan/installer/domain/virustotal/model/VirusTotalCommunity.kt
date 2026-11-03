// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.model

/** The only two verdicts accepted by VirusTotal's community voting endpoint. */
enum class VirusTotalCommunityVerdict(val apiValue: String) {
    HARMLESS("harmless"),
    MALICIOUS("malicious"),
}

/** Real VirusTotal community totals, not the number of antivirus engine detections. */
data class VirusTotalCommunityScores(
    val harmless: Int,
    val malicious: Int,
    val reputation: Int,
) {
    val totalVotes: Long get() = harmless.toLong() + malicious.toLong()
}
