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
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient

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
    fun `api key validation keeps the key out of the request url`() = runTest {
        server.enqueue(MockResponse(body = """{"data":{"type":"ip_address","id":"8.8.8.8"}}"""))
        val result = scanner.validateApiKey(KEY)
        val request = takeRequest()
        assertTrue(result.isSuccess, "A 200 response must validate the key")
        assertEquals("/api/v3/ip_addresses/8.8.8.8", request.url.encodedPath)
        assertTrue(!request.url.toString().contains(KEY), "The API key must never appear in a URL")
        assertEquals(KEY, request.headers["x-apikey"])
    }

    @Test
    fun `a missing report still proves the key was accepted`() = runTest {
        server.enqueue(MockResponse(code = 404, body = "{}"))
        assertTrue(scanner.validateApiKey(KEY).isSuccess)
    }

    @Test
    fun `an unauthorized validation response fails the key`() = runTest {
        server.enqueue(MockResponse(code = 401, body = "{}"))
        assertTrue(scanner.validateApiKey(KEY).isFailure)
    }

    @Test
    fun `small files use the standard endpoint and stream a stable hash`() = runTest {
        val file = tempFile().apply { writeText("small apk payload") }
        try {
            val hash = sha256(file)
            enqueueUploadAccepted()
            enqueueAnalysisCompleted()
            enqueueFileReport(hash)
            val report = scanner.uploadAndScan(KEY, DataEntity.FileEntity(file.path), "small.apk")
            val upload = takeRequest()
            assertEquals("POST", upload.method)
            assertEquals("/api/v3/files", upload.url.encodedPath)
            assertEquals(KEY, upload.headers["x-apikey"])
            assertTrue(upload.body!!.utf8().contains("small apk payload"))
            assertEquals("/api/v3/analyses/analysis-1", takeRequest().url.encodedPath)
            assertEquals("/api/v3/files/$hash", takeRequest().url.encodedPath)
            assertEquals(hash, report.sha256)
            assertEquals(VirusTotalScanMethod.UPLOAD, report.method)
            assertEquals(VirusTotalVerdict.CLEAN, report.verdict)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `files above 32 MB send the API key only to the trusted single-use URL`() = runTest {
        val size = VirusTotalUploadPolicy.DIRECT_MAX_BYTES + 1
        val file = tempFile()
        RandomAccessFile(file, "rw").use { it.setLength(size) }
        try {
            val hash = sha256(file)
            server.enqueue(MockResponse(body = """{"data":"${server.url("/_ah/upload/single-use")}"}"""))
            enqueueUploadAccepted()
            enqueueAnalysisCompleted()
            enqueueFileReport(hash)
            val report = scanner.uploadAndScan(KEY, DataEntity.FileEntity(file.path), "large.apk")
            val urlRequest = takeRequest()
            assertEquals("GET", urlRequest.method)
            assertEquals("/api/v3/files/upload_url", urlRequest.url.encodedPath)
            assertEquals(KEY, urlRequest.headers["x-apikey"])
            val upload = takeRequest()
            assertEquals("POST", upload.method)
            assertEquals("/_ah/upload/single-use", upload.url.encodedPath)
            assertEquals(KEY, upload.headers["x-apikey"])
            assertTrue(upload.bodySize > size)
            assertEquals("/api/v3/analyses/analysis-1", takeRequest().url.encodedPath)
            assertEquals("/api/v3/files/$hash", takeRequest().url.encodedPath)
            assertEquals(hash, report.sha256)
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
    fun `a rejected single-use upload is not reported as an invalid API key`() = runTest {
        val file = tempFile()
        RandomAccessFile(file, "rw").use { it.setLength(VirusTotalUploadPolicy.DIRECT_MAX_BYTES + 1) }
        val source = DataEntity.FileEntity(file.path)
        try {
            server.enqueue(MockResponse(body = """{"data":"${server.url("/_ah/upload/single-use")}"}"""))
            server.enqueue(MockResponse(code = 403, body = "{}"))
            val error = assertFailsWith<VirusTotalException> { scanner.uploadAndScan(KEY, source, "file.apk") }
            assertEquals(VirusTotalErrorKind.UPLOAD_REJECTED, error.kind)
            assertEquals(2, server.requestCount)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `single-use upload redirects are not followed with the API key`() = runTest {
        val file = tempFile()
        RandomAccessFile(file, "rw").use { it.setLength(VirusTotalUploadPolicy.DIRECT_MAX_BYTES + 1) }
        val source = DataEntity.FileEntity(file.path)
        try {
            server.enqueue(MockResponse(body = """{"data":"${server.url("/_ah/upload/single-use")}"}"""))
            server.enqueue(MockResponse(code = 302, headers = headersOf("Location", "https://untrusted.invalid/steal"), body = ""))
            assertFailsWith<VirusTotalException> { scanner.uploadAndScan(KEY, source, "file.apk") }
            assertEquals(2, server.requestCount)
            assertEquals("/api/v3/files/upload_url", takeRequest().url.encodedPath)
            val upload = takeRequest()
            assertEquals("/_ah/upload/single-use", upload.url.encodedPath)
            assertEquals(KEY, upload.headers["x-apikey"])
        } finally {
            file.delete()
        }
    }

    @Test
    fun `queued uploads can finish from the canonical file report instead of timing out`() = runTest {
        val file = tempFile().apply { writeText("already known payload") }
        try {
            val hash = sha256(file)
            enqueueUploadAccepted()
            enqueueAnalysisQueued()
            enqueueFileReport(hash)
            val report = scanner.uploadAndScan(KEY, DataEntity.FileEntity(file.path), "known.apk")
            assertEquals(VirusTotalScanMethod.UPLOAD, report.method)
            assertEquals("analysis-1", report.analysisId)
            assertEquals(hash, report.sha256)
            assertEquals(3, server.requestCount)
            assertEquals("/api/v3/files", takeRequest().url.encodedPath)
            assertEquals("/api/v3/analyses/analysis-1", takeRequest().url.encodedPath)
            assertEquals("/api/v3/files/$hash", takeRequest().url.encodedPath)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `new files keep polling after an empty canonical report until the analysis completes`() = runTest {
        val file = tempFile().apply { writeText("new payload") }
        try {
            val hash = sha256(file)
            enqueueUploadAccepted()
            enqueueAnalysisQueued()
            server.enqueue(MockResponse(code = 404, body = "{}"))
            enqueueAnalysisCompleted()
            enqueueFileReport(hash)
            val report = scanner.uploadAndScan(KEY, DataEntity.FileEntity(file.path), "new.apk")
            assertEquals(VirusTotalVerdict.CLEAN, report.verdict)
            assertEquals(5, server.requestCount)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `completed uploads restore engine names and community totals from the file report`() = runTest {
        val file = tempFile().apply { writeText("flagged payload") }
        try {
            val hash = sha256(file)
            enqueueUploadAccepted()
            enqueueAnalysisCompleted()
            enqueueFileReport(hash, detected = true)
            val report = scanner.uploadAndScan(KEY, DataEntity.FileEntity(file.path), "flagged.apk")
            assertEquals(1, report.positives)
            assertEquals(70, report.total)
            assertEquals("Example Engine", report.detections.single().engine)
            assertEquals("Android.Generic.Heuristic", report.detections.single().name)
            assertEquals(7, report.communityScores?.harmless)
            assertEquals(2, report.communityScores?.malicious)
            assertEquals(VirusTotalVerdict.LIKELY_FALSE_POSITIVE, report.verdict)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `hash lookup preserves detection names and reads real community totals`() = runTest {
        enqueueFileReport(HASH, detected = true)
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

    private fun enqueueUploadAccepted() {
        server.enqueue(MockResponse(body = """{"data":{"id":"analysis-1","type":"analysis"}}"""))
    }

    private fun enqueueAnalysisQueued() {
        server.enqueue(MockResponse(body = """{"data":{"id":"analysis-1","attributes":{"status":"queued"}}}"""))
    }

    private fun enqueueAnalysisCompleted() {
        server.enqueue(
            MockResponse(body = """{"data":{"id":"analysis-1","attributes":{"status":"completed","stats":{"harmless":70},"results":{}}}}"""),
        )
    }

    private fun enqueueFileReport(hash: String, detected: Boolean = false) {
        val stats = if (detected) """{"malicious":1,"harmless":69}""" else """{"harmless":70}"""
        val results = if (detected) {
            """"last_analysis_results":{"engine":{"category":"malicious","engine_name":"Example Engine","result":"Android.Generic.Heuristic"}},"""
        } else {
            ""
        }
        server.enqueue(
            MockResponse(
                body = """{"data":{"attributes":{"sha256":"$hash","last_analysis_stats":$stats,$results"total_votes":{"harmless":7,"malicious":2},"reputation":5}}}""",
            ),
        )
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
