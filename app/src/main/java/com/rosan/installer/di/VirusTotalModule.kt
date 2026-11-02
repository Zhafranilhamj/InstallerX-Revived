// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.virustotal.VirusTotalScannerImpl
import com.rosan.installer.domain.virustotal.VirusTotalScanner
import com.rosan.installer.domain.virustotal.usecase.ScanFileByHashUseCase
import com.rosan.installer.domain.virustotal.usecase.UploadFileForScanUseCase
import com.rosan.installer.domain.virustotal.usecase.ValidateVirusTotalApiKeyUseCase
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val CONNECT_TIMEOUT_SECONDS = 30L
private const val READ_TIMEOUT_SECONDS = 60L
private const val WRITE_TIMEOUT_SECONDS = 120L

val virusTotalModule = module {
    // VirusTotal uploads can be much slower than the shared API client's 15s budget.
    // A dedicated client keeps large APK uploads from failing on a slow connection.
    single(named("VirusTotalOkHttpClient")) {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .build()
    }

    single<VirusTotalScanner> {
        VirusTotalScannerImpl(
            client = get(named("VirusTotalOkHttpClient")),
            json = get(),
        )
    }

    factoryOf(::ValidateVirusTotalApiKeyUseCase)
    factoryOf(::ScanFileByHashUseCase)
    factoryOf(::UploadFileForScanUseCase)
}
