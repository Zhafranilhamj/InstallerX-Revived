// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import com.rosan.installer.domain.share.model.LocalShareProtocol
import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink

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
        val client = OkHttpClient.Builder().followRedirects(false).build()
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
            // The cookie must not be a deterministic function of the PIN.
            assertNotEquals(
                "${LocalShareProtocol.PIN_COOKIE}=1234",
                cookie,
            )

            val csrf = client.newCall(Request.Builder().url("$base/").header("Cookie", cookie).build())
                .execute().use { response ->
                    assertEquals(200, response.code)
                    val body = response.body.string()
                    assertTrue(body.contains("InstallerX local sharing"))
                    body.lineSequence()
                        .firstOrNull { it.contains("csrf-token") }
                        ?.substringAfter("content=\"")
                        ?.substringBefore('"')
                }
            assertTrue(!csrf.isNullOrBlank(), "Dashboard must expose a per-run CSRF token")

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
                    .header("X-CSRF-Token", csrf!!)
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
            OkHttpClient.Builder().followRedirects(false).build().newCall(
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

    @Test
    fun `upload of exactly the size limit is accepted`() {
        withServer(maxUploadBytes = UPLOAD_LIMIT) { server, directory ->
            val body = ByteArray(UPLOAD_LIMIT.toInt()) { 'a'.code.toByte() }
            assertEquals(200, upload(server, "at-limit.apk", body).first)
            assertEquals(1, directory.listFiles()?.count { it.name.endsWith(".apk") } ?: 0)
        }
    }

    @Test
    fun `upload one byte over the size limit is rejected with 413`() {
        withServer(maxUploadBytes = UPLOAD_LIMIT) { server, directory ->
            val body = ByteArray(UPLOAD_LIMIT.toInt() + 1) { 'a'.code.toByte() }
            assertEquals(413, upload(server, "too-big.apk", body).first)
            assertTrue(
                directory.listFiles().orEmpty().none { it.name.endsWith(".apk") || it.name.endsWith(".part") },
                "Oversized uploads must not leave a partial file behind",
            )
        }
    }

    @Test
    fun `chunked uploads are rejected with 411 instead of being half decoded`() {
        withServer(maxUploadBytes = UPLOAD_LIMIT) { server, _ ->
            val chunked = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength(): Long = -1L
                override fun writeTo(sink: BufferedSink) {
                    sink.write("package-bytes".toByteArray())
                }
            }
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "chunked.apk", chunked)
                .build()
            assertEquals(411, upload(server, "chunked.apk", multipart).first)
        }
    }

    @Test
    fun `a cross-site post is refused even when the pin is off`() {
        withServer(maxUploadBytes = UPLOAD_LIMIT) { server, directory ->
            val multipart = plainMultipart("evil.apk", "payload".toByteArray())
            val response = OkHttpClient.Builder().followRedirects(false).build().newCall(
                Request.Builder()
                    .url("http://127.0.0.1:${server.port}/upload")
                    .header("Origin", "https://evil.example")
                    .post(multipart)
                    .build(),
            ).execute().use { it.code }
            assertEquals(403, response)
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        }
    }

    @Test
    fun `a session upload without the csrf token is refused`() {
        withServer(maxUploadBytes = UPLOAD_LIMIT, pin = "1234") { server, directory ->
            val client = OkHttpClient.Builder().followRedirects(false).build()
            val base = "http://127.0.0.1:${server.port}"
            val cookie = client.newCall(
                Request.Builder()
                    .url("$base/login")
                    .post("pin=1234".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    .build(),
            ).execute().use { it.header("Set-Cookie").orEmpty().substringBefore(';') }

            val multipart = plainMultipart("csrf.apk", "payload".toByteArray())
            val code = client.newCall(
                Request.Builder()
                    .url("$base/upload")
                    .header("Cookie", cookie)
                    .post(multipart)
                    .build(),
            ).execute().use { it.code }
            assertEquals(403, code)
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        }
    }

    @Test
    fun `percent-encoded pin query parameter still authenticates`() {
        withServer(maxUploadBytes = UPLOAD_LIMIT, pin = "1234") { server, _ ->
            val code = OkHttpClient.Builder().followRedirects(false).build().newCall(
                Request.Builder()
                    .url("http://127.0.0.1:${server.port}/api/status?pin=1%32%33%34")
                    .build(),
            ).execute().use { it.code }
            assertEquals(200, code)
        }
    }

    @Test
    fun `only package container extensions are accepted as upload names`() {
        withServer(maxUploadBytes = UPLOAD_LIMIT) { server, directory ->
            assertEquals(400, upload(server, "payload.sh", "#!/bin/sh".toByteArray()).first)
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        }
    }

    @Test
    fun `requests are refused when the network switch is off`() {
        val port = ServerSocket(0).use { it.localPort }
        val directory = Files.createTempDirectory("installerx-offline").toFile()
        val server = LocalHttpServer(
            port = port,
            pin = "",
            receiveDirectory = directory,
            versionName = "test",
            onFileReceived = {},
            onError = {},
            networkAllowed = { false },
        )
        server.start()
        try {
            OkHttpClient.Builder().followRedirects(false).build().newCall(
                Request.Builder().url("http://127.0.0.1:$port/api/status").build(),
            ).execute().use { response ->
                assertEquals(503, response.code)
            }
        } finally {
            server.stop()
            directory.deleteRecursively()
        }
    }

    private fun withServer(
        maxUploadBytes: Long,
        pin: String = "",
        block: (server: LocalHttpServer, directory: File) -> Unit,
    ) {
        val port = ServerSocket(0).use { it.localPort }
        val directory = Files.createTempDirectory("installerx-upload").toFile()
        val server = LocalHttpServer(
            port = port,
            pin = pin,
            receiveDirectory = directory,
            versionName = "test",
            onFileReceived = {},
            onError = {},
            maxUploadBytes = maxUploadBytes,
        )
        server.start()
        try {
            block(server, directory)
        } finally {
            server.stop()
            directory.deleteRecursively()
        }
    }

    private fun plainMultipart(name: String, bytes: ByteArray): MultipartBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", name, bytes.toRequestBody("application/octet-stream".toMediaType()))
        .build()

    private fun upload(server: LocalHttpServer, name: String, bytes: ByteArray): Pair<Int, String> =
        upload(server, name, plainMultipart(name, bytes))

    private fun upload(server: LocalHttpServer, name: String, body: RequestBody): Pair<Int, String> {
        val response = OkHttpClient.Builder().followRedirects(false).build().newCall(
            Request.Builder()
                .url("http://127.0.0.1:${server.port}/upload")
                .post(body)
                .build(),
        ).execute()
        return response.use { it.code to it.body.string() }
    }

    private companion object {
        const val UPLOAD_LIMIT = 1024L
    }
}
