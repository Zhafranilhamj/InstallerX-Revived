// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.share.model.LocalShareConfig
import com.rosan.installer.domain.share.model.LocalShareEndpoint
import com.rosan.installer.domain.share.model.LocalShareState
import com.rosan.installer.ui.activity.InstallerActivity
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Owns the built-in HTTP server and the "Send to TV" QR handshake state. */
class LocalShareManager(
    private val context: Context,
    private val configStore: LocalShareConfigStore,
    private val appSettingsRepository: AppSettingsRepository,
) {
    private val _state = MutableStateFlow(LocalShareState(config = configStore.load()))
    val state: StateFlow<LocalShareState> = _state.asStateFlow()

    private val _pendingEndpoint = MutableStateFlow<LocalShareEndpoint?>(null)
    val pendingEndpoint: StateFlow<LocalShareEndpoint?> = _pendingEndpoint.asStateFlow()

    @Volatile
    private var server: LocalHttpServer? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Invoked when the network policy stops the server, so the service can tear itself down. */
    @Volatile
    var onStoppedByNetworkPolicy: (() -> Unit)? = null

    /**
     * Cached copy of the in-app network switch. The HTTP server calls this from worker threads for
     * every request, so it must not suspend.
     */
    private val networkAllowed = AtomicBoolean(false)

    init {
        scope.launch {
            appSettingsRepository.preferencesFlow
                .map { it.allowInternetAccess }
                .distinctUntilChanged()
                .collect { allowed ->
                    networkAllowed.set(allowed)
                    if (!allowed && server != null) {
                        // Never keep listening once the user revokes network access: an open LAN
                        // port would still accept uploads.
                        Timber.i("Network access revoked; stopping the local sharing server")
                        stop()
                        onStoppedByNetworkPolicy?.invoke()
                    }
                }
        }
    }

    fun start(config: LocalShareConfig): Result<Unit> {
        if (!config.valid) return Result.failure(IllegalArgumentException("Invalid port or PIN"))
        // The runtime network boundary also covers accepting local-network connections.
        if (!networkAllowed.get()) {
            return Result.failure(IllegalStateException("Network access is disabled in settings"))
        }
        stop()
        configStore.save(config)
        val directory = File(context.filesDir, "local_share")
        return runCatching {
            val instance = LocalHttpServer(
                port = config.port,
                pin = config.pin,
                receiveDirectory = directory,
                versionName = AppConfig.VERSION_NAME,
                onFileReceived = ::onFileReceived,
                onError = { message -> _state.update { it.copy(lastError = message) } },
                networkAllowed = { networkAllowed.get() },
            )
            instance.start()
            server = instance
            _state.update {
                it.copy(
                    running = true,
                    config = config,
                    addresses = localAddresses(),
                    receivedFiles = directory.listFiles()?.filter { file -> file.isFile }?.map { file -> file.name }.orEmpty(),
                    lastError = null,
                )
            }
        }.onFailure { error ->
            Timber.w(error, "Failed to start local sharing server")
            _state.update { it.copy(running = false, lastError = error.message) }
        }
    }

    fun stop() {
        runCatching { server?.stop() }
        server = null
        _state.update { it.copy(running = false) }
    }

    fun setPendingEndpoint(endpoint: LocalShareEndpoint) {
        _pendingEndpoint.value = endpoint
    }

    fun consumePendingEndpoint(): LocalShareEndpoint? {
        val endpoint = _pendingEndpoint.value
        _pendingEndpoint.value = null
        return endpoint
    }

    fun localAddresses(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filter { address ->
                address is Inet4Address && !address.isLoopbackAddress &&
                    com.rosan.installer.domain.share.model.LocalShareProtocol.isLocalPeer(address.hostAddress.orEmpty())
            }
            .mapNotNull { it.hostAddress }
            .distinct()
            .sorted()
            .toList()
    }.getOrDefault(emptyList())

    private fun onFileReceived(file: File) {
        _state.update {
            it.copy(receivedFiles = (listOf(file.name) + it.receivedFiles).distinct().take(50))
        }
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() ?: return
        val intent = Intent(context, InstallerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "package", uri)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { error -> Timber.w(error, "Could not open received package ${file.name}") }
    }
}
