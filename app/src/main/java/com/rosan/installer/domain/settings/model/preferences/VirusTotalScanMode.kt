// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences

/**
 * Controls how files are checked against VirusTotal before installation.
 *
 * - [OFF]: no VirusTotal request is made.
 * - [NORMAL]: scanning is optional; the user decides whether to check a file.
 * - [STRICT]: every file is uploaded and installation waits for the scan to finish.
 */
enum class VirusTotalScanMode(val value: String) {
    OFF("Off"),
    NORMAL("Normal"),
    STRICT("Strict");

    companion object {
        fun fromValueOrDefault(value: String?): VirusTotalScanMode =
            entries.firstOrNull { it.value == value } ?: OFF
    }
}
