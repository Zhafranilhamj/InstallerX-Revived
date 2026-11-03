// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.virustotal

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityVerdict
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalUploadPolicy
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import com.rosan.installer.domain.virustotal.toHexString
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import mockwebserver3.RecordedRequest

class VirusTotalScannerProtocolTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var scanner: VirusTotalScannerImpl
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        scanner = VirusTotalScannerImpl(client, json, server.url("/api/v3").toString(), pollIntervalMs = 1)
    }

    @AfterTest
    fun tearDown() {
        server.close()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    @Test
    fun `small files use the standard endpoint and stream a stable hash`() = runTest {
        val file = tempFile().apply { writeText("small apk payload") }
        try {
            enqueueAnalysis()
            val report = scanner.uploadAndScan(KEY, DataEntity.FileEntity(file.path), "small.apk")
            val upload = takeRequest()
            assertEquals("POST", upload.method)
            assertEquals("/api/v3/files", upload.url.encodedPath)
            assertEquals(KEY, upload.headers["x-apikey"])
            assertTrue(upload.body!!.utf8().contains("small apk payload"))
            assertEquals("/api/v3/analyses/analysis-1", takeRequest().url.encodedPath)
            assertEquals(sha256(file), report.sha256)
            assertEquals(VirusTotalScanMethod.UPLOAD, report.method)
            assertEquals(VirusTotalVerdict.CLEAN, report.verdict)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `files above 32 MB first request a single-use URL without forwarding the API key`() = runTest {
        val size = VirusTotalUploadPolicy.DIRECT_MAX_BYTES + 1
        val file = tempFile()
        RandomAccessFile(file, "rw").use { it.setLength(size) }
        try {
            server.enqueue(MockResponse(body = """{"data":"${server.url("/_ah/upload/single-use")}"}"""))
            enqueueAnalysis()
            val report = scanner.uploadAndScan(KEY, DataEntity.FileEntity(file.path), "large.apk")
            val urlRequest = takeRequest()
            assertEquals("GET", urlRequest.method)
            assertEquals("/api/v3/files/upload_url", urlRequest.url.encodedPath)
            assertEquals(KEY, urlRequest.headers["x-apikey"])
            val upload = takeRequest()
            assertEquals("POST", upload.method)
            assertEquals("/_ah/upload/single-use", upload.url.encodedPath)
            assertNull(upload.headers["x-apikey"])
            assertTrue(upload.bodySize > size)
            assertEquals("/api/v3/analyses/analysis-1", takeRequest().url.encodedPath)
            assertEquals(sha256(file), report.sha256)
            assertEquals(size, report.fileSize)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `oversized and unknown-size files never reach either upload endpoint`() = runTest {
        val file = tempFile().apply { writeText("not uploaded") }
        try {
            for ((size, kind) in listOf(VirusTotalUploadPolicy.MAX_UPLOAD_BYTES + 1 to VirusTotalErrorKind.FILE_TOO_LARGE, -1L to VirusTotalErrorKind.FILE_UNREADABLE)) {
                val source = object : DataEntity.FileEntity(file.path) {
                    override fun getSize(): Long = size
                }
                val error = assertFailsWith<VirusTotalException> { scanner.uploadAndScan(KEY, source, "too-large.apk") }
                assertEquals(kind, error.kind)
            }
            assertEquals(0, server.requestCount)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `upload URL errors and untrusted destinations stop before uploading`() = runTest {
        val file = tempFile().apply { writeText("private payload") }
        val source = object : DataEntity.FileEntity(file.path) {
            override fun getSize(): Long = VirusTotalUploadPolicy.DIRECT_MAX_BYTES + 1
        }
        try {
            server.enqueue(MockResponse(code = 403, body = "{}"))
            assertEquals(VirusTotalErrorKind.INVALID_API_KEY, assertFailsWith<VirusTotalException> { scanner.uploadAndScan(KEY, source, "file.apk") }.kind)
            assertEquals(1, server.requestCount)
            server.enqueue(MockResponse(body = """{"data":"https://untrusted.invalid/upload"}"""))
            assertEquals(VirusTotalErrorKind.UNKNOWN, assertFailsWith<VirusTotalException> { scanner.uploadAndScan(KEY, source, "file.apk") }.kind)
            assertEquals(2, server.requestCount)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `hash lookup preserves detection names and reads real community totals`() = runTest {
        server.enqueue(
            MockResponse(
                body = """{"data":{"attributes":{"sha256":"$HASH","last_analysis_stats":{"malicious":1,"harmless":69},"last_analysis_results":{"engine":{"category":"malicious","engine_name":"Example Engine","result":"Android.Generic.Heuristic"}},"total_votes":{"harmless":7,"malicious":2},"reputation":5}}}""",
            ),
        )
        val report = scanner.lookupByHash(KEY, HASH)
        assertEquals(VirusTotalVerdict.LIKELY_FALSE_POSITIVE, report.verdict)
        assertEquals("Example Engine", report.detections.single().engine)
        assertEquals("Android.Generic.Heuristic", report.detections.single().name)
        assertEquals(7, report.communityScores?.harmless)
        assertEquals(2, report.communityScores?.malicious)
        assertEquals(5, report.communityScores?.reputation)
        assertEquals("/api/v3/files/$HASH", takeRequest().url.encodedPath)
    }

    @Test
    fun `both vote verdicts use the documented JSON envelope`() = runTest {
        for (verdict in VirusTotalCommunityVerdict.entries) {
            server.enqueue(MockResponse(body = """{"data":{"type":"vote","attributes":{"verdict":"${verdict.apiValue}"}}}"""))
            scanner.voteOnFile(KEY, HASH, verdict)
            val request = takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v3/files/$HASH/votes", request.url.encodedPath)
            assertEquals(KEY, request.headers["x-apikey"])
            assertTrue(request.headers["Content-Type"]!!.startsWith("application/json"))
            val data = json.parseToJsonElement(request.body!!.utf8()).jsonObject.getValue("data").jsonObject
            assertEquals("vote", data.getValue("type").jsonPrimitive.content)
            assertEquals(verdict.apiValue, data.getValue("attributes").jsonObject.getValue("verdict").jsonPrimitive.content)
        }
    }

    @Test
    fun `failed votes and missing community totals are not reported as success or zero votes`() = runTest {
        server.enqueue(MockResponse(code = 403, body = "{}"))
        assertEquals(VirusTotalErrorKind.VOTE_FAILED, assertFailsWith<VirusTotalException> { scanner.voteOnFile(KEY, HASH, VirusTotalCommunityVerdict.HARMLESS) }.kind)
        server.enqueue(MockResponse(body = """{"data":{"attributes":{"last_analysis_stats":{"harmless":70}}}}"""))
        assertEquals(VirusTotalErrorKind.COMMUNITY_UNAVAILABLE, assertFailsWith<VirusTotalException> { scanner.getCommunityScores(KEY, HASH) }.kind)
        server.enqueue(MockResponse(body = """{"data":{"attributes":{"total_votes":{"harmless":0,"malicious":0},"reputation":-2}}}"""))
        val scores = scanner.getCommunityScores(KEY, HASH)
        assertEquals(0L, scores.totalVotes)
        assertEquals(-2, scores.reputation)
    }

    private fun enqueueAnalysis() {
        server.enqueue(MockResponse(body = """{"data":{"id":"analysis-1","type":"analysis"}}"""))
        server.enqueue(MockResponse(body = """{"data":{"id":"analysis-1","attributes":{"status":"completed","stats":{"harmless":70},"results":{}}}}"""))
    }

    private fun takeRequest(): RecordedRequest = assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))

    private fun tempFile(): File = File.createTempFile("virustotal-test-", ".apk")

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHexString()
    }

    private companion object {
        const val KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        const val HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
