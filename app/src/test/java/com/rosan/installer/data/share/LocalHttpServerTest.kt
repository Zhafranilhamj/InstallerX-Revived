// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import com.rosan.installer.domain.share.model.LocalShareProtocol
import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class LocalHttpServerTest {
    @Test
    fun `dashboard enforces the pin and streams a multipart upload to the installer callback`() {
        val port = ServerSocket(0).use { it.localPort }
        val directory = Files.createTempDirectory("installerx-share").toFile()
        val received = mutableListOf<File>()
        val server = LocalHttpServer(
            port = port,
            pin = "1234",
            receiveDirectory = directory,
            versionName = "test",
            onFileReceived = { received += it },
            onError = {},
        )
        server.start()
        val client = OkHttpClient()
        try {
            val base = "http://127.0.0.1:$port"
            client.newCall(Request.Builder().url("$base/").build()).execute().use { response ->
                assertEquals(401, response.code)
                assertTrue(response.body.string().contains("PIN"))
            }

            val loginBody = "pin=1234".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val cookie = client.newCall(
                Request.Builder().url("$base/login").post(loginBody).build(),
            ).execute().use { response ->
                assertEquals(303, response.code)
                response.header("Set-Cookie").orEmpty().substringBefore(';')
            }
            assertTrue(cookie.startsWith("${LocalShareProtocol.PIN_COOKIE}="))

            client.newCall(Request.Builder().url("$base/").header("Cookie", cookie).build()).execute().use { response ->
                assertEquals(200, response.code)
                assertTrue(response.body.string().contains("InstallerX local sharing"))
            }

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "sample.apk",
                    "package-bytes".toRequestBody("application/octet-stream".toMediaType()),
                )
                .build()
            client.newCall(
                Request.Builder()
                    .url("$base/upload")
                    .header("Cookie", cookie)
                    .post(multipart)
                    .build(),
            ).execute().use { response ->
                assertEquals(200, response.code)
                assertTrue(response.body.string().contains("\"ok\":true"))
            }
            assertEquals(1, received.size)
            assertEquals("sample.apk", received.first().name)
            assertEquals("package-bytes", received.first().readText())

            client.newCall(
                Request.Builder().url("$base/api/files").header("Cookie", cookie).build(),
            ).execute().use { response ->
                assertTrue(response.body.string().contains("sample.apk"))
            }
        } finally {
            server.stop()
            directory.deleteRecursively()
        }
    }

    @Test
    fun `requests without a pin are accepted when security is off`() {
        val port = ServerSocket(0).use { it.localPort }
        val directory = Files.createTempDirectory("installerx-open").toFile()
        val server = LocalHttpServer(port, "", directory, "test", {}, {})
        server.start()
        try {
            OkHttpClient().newCall(
                Request.Builder().url("http://127.0.0.1:$port/api/status").build(),
            ).execute().use { response ->
                assertEquals(200, response.code)
                assertTrue(response.body.string().contains("\"running\":true"))
            }
        } finally {
            server.stop()
            directory.deleteRecursively()
        }
    }
}
