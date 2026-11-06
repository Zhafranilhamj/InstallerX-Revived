// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.appmanager.InstalledAppsRepositoryImpl
import com.rosan.installer.data.appmanager.UninstallStatusHolder
import com.rosan.installer.data.downloader.DownloadHistoryStore
import com.rosan.installer.data.downloader.RemoteDownloaderRepositoryImpl
import com.rosan.installer.data.obb.ObbDeploymentRepositoryImpl
import com.rosan.installer.data.obb.ObbPayloadExtractor
import com.rosan.installer.data.obb.ObbSafTreeStore
import com.rosan.installer.data.obb.ObbWriter
import com.rosan.installer.data.share.LocalShareClient
import com.rosan.installer.data.share.LocalShareConfigStore
import com.rosan.installer.data.share.LocalShareManager
import com.rosan.installer.domain.appmanager.repository.InstalledAppsRepository
import com.rosan.installer.domain.downloader.repository.DownloadRepository
import com.rosan.installer.domain.obb.repository.ObbDeploymentRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/** Wiring for OBB deployment, local sharing, the app manager and the remote downloader. */
val toolsModule = module {
    // OBB
    singleOf(::ObbSafTreeStore)
    singleOf(::ObbPayloadExtractor)
    singleOf(::ObbWriter)
    singleOf(::ObbDeploymentRepositoryImpl) { bind<ObbDeploymentRepository>() }

    // Local sharing / TV push
    singleOf(::LocalShareConfigStore)
    singleOf(::LocalShareManager)
    singleOf(::LocalShareClient)

    // App manager
    singleOf(::UninstallStatusHolder)
    singleOf(::InstalledAppsRepositoryImpl) { bind<InstalledAppsRepository>() }

    // Remote downloader
    singleOf(::DownloadHistoryStore)
    singleOf(::RemoteDownloaderRepositoryImpl) { bind<DownloadRepository>() }
}
