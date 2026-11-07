// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.share.model.LocalShareEndpoint
import com.rosan.installer.domain.share.model.LocalShareProtocol
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink

/** Uploads a package from this device to another device running the local sharing server. */
class LocalShareClient(private val appSettingsRepository: AppSettingsRepository) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    suspend fun push(endpoint: LocalShareEndpoint, file: File): Result<String> = withContext(Dispatchers.IO) {
        if (!appSettingsRepository.preferencesFlow.first().allowInternetAccess) {
            return@withContext Result.failure(IllegalStateException("Network access is disabled in settings"))
        }
        runCatching {
            // The platform network security config cannot restrict cleartext to LAN ranges, so the
            // destination is validated here instead: never upload a package to a WAN address.
            requireLocalPeer(endpoint.baseUrl)
            // The PIN travels in a header only. Putting it in the query string leaked the secret
            // into proxy logs, crash reports and any intermediate tooling.
            val url = "${endpoint.baseUrl}${LocalShareProtocol.UPLOAD_PATH}"
            val body = object : RequestBody() {
                override fun contentType(): MediaType? = null
                override fun contentLength(): Long = file.length()

                override fun writeTo(sink: BufferedSink) {
                    file.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            sink.write(buffer, 0, count)
                        }
                    }
                }
            }
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, body)
                .build()
            val request = Request.Builder()
                .url(url)
                .header(LocalShareProtocol.PIN_HEADER, endpoint.pin)
                .post(multipart)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body.string()
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $text")
                text
            }
        }
    }

    /**
     * Rejects any destination that is not loopback/RFC1918/link-local/ULA.
     *
     * Resolving the name first means an attacker-controlled DNS record cannot redirect the upload
     * to a public endpoint, and `localhost` aliases are handled uniformly.
     */
    private fun requireLocalPeer(baseUrl: String) {
        val host = runCatching { URI(baseUrl).host }.getOrNull()
            ?: throw IOException("Malformed sharing address")
        val normalized = host.lowercase().removePrefix("[").removeSuffix("]")
        if (normalized == "localhost") return
        val addresses = InetAddress.getAllByName(normalized)
        if (addresses.isEmpty()) throw IOException("Could not resolve the sharing host")
        for (address in addresses) {
            if (!LocalShareProtocol.isLocalPeer(address.hostAddress.orEmpty())) {
                throw IOException("Refusing to send a package to a non-local address")
            }
        }
    }
}
