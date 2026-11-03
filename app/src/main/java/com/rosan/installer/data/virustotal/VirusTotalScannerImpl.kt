// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.virustotal

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.virustotal.VirusTotalScanner
import com.rosan.installer.domain.virustotal.model.VirusTotalApiKeyFormat
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityScores
import com.rosan.installer.domain.virustotal.model.VirusTotalCommunityVerdict
import com.rosan.installer.domain.virustotal.model.VirusTotalDetection
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.model.VirusTotalFalsePositiveHeuristics
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalUploadPolicy
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import com.rosan.installer.domain.virustotal.toHexString
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import timber.log.Timber

/**
 * VirusTotal API v3 client. The API key is never logged and is sent only to the API, never to a
 * single-use upload URL. Files are streamed with a bounded buffer, including large uploads.
 *
 * [baseUrl] and [pollIntervalMs] are constructor seams for local protocol tests; production uses
 * the HTTPS VirusTotal API through Koin's default arguments.
 */
class VirusTotalScannerImpl(
    private val client: OkHttpClient,
    private val json: Json,
    baseUrl: String = BASE_URL,
    private val pollIntervalMs: Long = POLL_INTERVAL_MS,
) : VirusTotalScanner {
    private val apiBaseUrl = baseUrl.trimEnd('/')
    private val apiBase = requireNotNull(apiBaseUrl.toHttpUrlOrNull()) { "Invalid VirusTotal API URL" }

    // Share the connection pool, but do not forward a signed upload to a redirect destination.
    private val uploadClient by lazy {
        client.newBuilder().followRedirects(false).followSslRedirects(false).build()
    }

    override suspend fun validateApiKey(apiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!VirusTotalApiKeyFormat.isValid(apiKey)) {
            return@withContext Result.failure(
                VirusTotalException(
                    kind = VirusTotalErrorKind.INVALID_API_KEY,
                    message = "A VirusTotal API key must be 64 hexadecimal characters.",
                ),
            )
        }
        try {
            // This endpoint also works for accounts that cannot query a particular file's scores.
            apiGet(apiKey, "/users/$apiKey/overall_quotas", "while validating the API key")
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            Result.failure(error)
        }
    }

    override suspend fun lookupByHash(apiKey: String, sha256: String): VirusTotalReport = withContext(Dispatchers.IO) {
        val normalizedHash = normalizeHash(sha256)
        val body = apiGet(
            apiKey = apiKey,
            path = "/files/$normalizedHash",
            operation = "while looking up the file",
            notFoundMessage = "VirusTotal has no report for this hash yet.",
        )
        val attributes = decode<VtFileResponse>(body).data?.attributes
        val stats = attributes?.last_analysis_stats
        if (attributes == null || stats == null) {
            throw VirusTotalException(VirusTotalErrorKind.NOT_FOUND, "VirusTotal has no report for this hash yet.")
        }
        parseReport(
            stats = stats,
            results = attributes.last_analysis_results,
            sha256 = attributes.sha256 ?: normalizedHash,
            fileName = attributes.meaningful_name,
            fileSize = attributes.size,
            method = VirusTotalScanMethod.HASH,
            communityScores = attributes.communityScores(),
        )
    }

    override suspend fun uploadAndScan(
        apiKey: String,
        data: DataEntity,
        fileName: String,
    ): VirusTotalReport = withContext(Dispatchers.IO) {
        val size = data.getSize()
        VirusTotalUploadPolicy.validationError(size)?.let { kind ->
            throw VirusTotalException(
                kind = kind,
                message = when (kind) {
                    VirusTotalErrorKind.FILE_TOO_LARGE -> "The file is larger than the 650 MB upload limit."
                    else -> "The file size could not be determined for upload."
                },
            )
        }

        val uploadUrl = if (VirusTotalUploadPolicy.requiresUploadUrl(size)) {
            val response = apiGet(apiKey, "/files/upload_url", "while requesting an upload URL")
            validateUploadUrl(decode<VtUploadUrlResponse>(response).data)
        } else {
            "$apiBaseUrl/files"
        }

        val uploadJob = currentCoroutineContext()[Job]
        val digest = MessageDigest.getInstance("SHA-256")
        var uploadSha256: String? = null
        val uploadBody = object : RequestBody() {
            override fun contentType(): MediaType = OCTET_STREAM
            override fun contentLength(): Long = size

            // Never replay an upload automatically: it may contain private files, and the large
            // upload URL is single-use. A failed upload must be retried explicitly by the user.
            override fun isOneShot(): Boolean = true

            override fun writeTo(sink: BufferedSink) {
                val input = data.getInputStream() ?: throw IOException("The selected file could not be opened for upload.")
                digest.reset()
                var written = 0L
                val buffer = ByteArray(UPLOAD_BUFFER_SIZE)
                input.use { stream ->
                    while (written < size) {
                        if (uploadJob?.isCancelled == true) throw CancellationException("VirusTotal upload cancelled")
                        val count = stream.read(buffer, 0, minOf(buffer.size.toLong(), size - written).toInt())
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                        sink.write(buffer, 0, count)
                        written += count
                    }
                    if (written != size || stream.read() != -1) {
                        throw IOException("The selected file changed size while it was being uploaded.")
                    }
                }
                uploadSha256 = digest.digest().toHexString()
            }
        }
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, uploadBody)
            .build()
        val uploadRequest = Request.Builder().url(uploadUrl).apply {
            if (!VirusTotalUploadPolicy.requiresUploadUrl(size)) header(API_KEY_HEADER, apiKey)
        }.post(multipart).build()
        val uploadResponse = executeBody(uploadRequest, "while uploading", VirusTotalErrorKind.INVALID_API_KEY, httpClient = uploadClient)
        val analysisId = decode<VtAnalysisResponse>(uploadResponse).data?.id
            ?: throw VirusTotalException(VirusTotalErrorKind.UNKNOWN, "VirusTotal did not return an analysis id.")
        val analysisSha256 = uploadSha256
            ?: throw VirusTotalException(VirusTotalErrorKind.UNKNOWN, "The file upload did not complete.")

        val deadline = System.currentTimeMillis() + MAX_ANALYSIS_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            delay(pollIntervalMs)
            currentCoroutineContext().ensureActive()
            val body = apiGet(apiKey, "/analyses/$analysisId", "while polling")
            val attributes = decode<VtAnalysisResponse>(body).data?.attributes
            when (attributes?.status) {
                "completed" -> {
                    val stats = attributes.stats ?: throw VirusTotalException(
                        VirusTotalErrorKind.UNKNOWN,
                        "VirusTotal completed the analysis without statistics.",
                    )
                    return@withContext parseReport(
                        stats = stats,
                        results = attributes.results,
                        sha256 = analysisSha256,
                        fileName = fileName,
                        fileSize = size,
                        method = VirusTotalScanMethod.UPLOAD,
                        analysisId = analysisId,
                    )
                }

                "failed" -> throw VirusTotalException(VirusTotalErrorKind.UNKNOWN, "VirusTotal failed to analyze this file.")

                else -> Unit
            }
        }
        throw VirusTotalException(VirusTotalErrorKind.TIMEOUT, "The VirusTotal analysis did not finish in time.")
    }

    override suspend fun getCommunityScores(apiKey: String, sha256: String): VirusTotalCommunityScores = withContext(Dispatchers.IO) {
        val body = apiGet(
            apiKey = apiKey,
            path = "/files/${normalizeHash(sha256)}",
            operation = "while loading community scores",
            authorizationError = VirusTotalErrorKind.COMMUNITY_UNAVAILABLE,
        )
        decode<VtFileResponse>(body).data?.attributes?.communityScores()
            ?: throw VirusTotalException(VirusTotalErrorKind.COMMUNITY_UNAVAILABLE, "Community scores are unavailable.")
    }

    override suspend fun voteOnFile(apiKey: String, sha256: String, verdict: VirusTotalCommunityVerdict) {
        withContext(Dispatchers.IO) {
            val payload = VtVoteRequest(VtVoteData(VtVoteAttributes(verdict.apiValue)))
            val request = Request.Builder()
                .url("$apiBaseUrl/files/${normalizeHash(sha256)}/votes")
                .header(API_KEY_HEADER, apiKey)
                .post(json.encodeToString(VtVoteRequest.serializer(), payload).toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val body = executeBody(request, "while submitting a vote", VirusTotalErrorKind.VOTE_FAILED)
            val response = decode<VtVoteResponse>(body)
            if (response.data?.attributes?.verdict != verdict.apiValue) {
                throw VirusTotalException(VirusTotalErrorKind.VOTE_FAILED, "VirusTotal did not confirm the vote.")
            }
        }
    }

    private suspend fun apiGet(
        apiKey: String,
        path: String,
        operation: String,
        authorizationError: VirusTotalErrorKind = VirusTotalErrorKind.INVALID_API_KEY,
        notFoundMessage: String = "The VirusTotal resource could not be found.",
    ): String {
        val request = Request.Builder().url("$apiBaseUrl$path").header(API_KEY_HEADER, apiKey).build()
        return executeBody(request, operation, authorizationError, notFoundMessage)
    }

    private suspend fun executeBody(
        request: Request,
        operation: String,
        authorizationError: VirusTotalErrorKind,
        notFoundMessage: String = "The VirusTotal resource could not be found.",
        httpClient: OkHttpClient = client,
    ): String = try {
        httpClient.await(request).use { response ->
            when (response.code) {
                HTTP_OK -> response.body.string().ifEmpty {
                    throw VirusTotalException(VirusTotalErrorKind.UNKNOWN, "VirusTotal returned an empty response.")
                }

                HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> throw VirusTotalException(
                    authorizationError,
                    "VirusTotal denied this request. Check the API key and its permissions.",
                )

                HTTP_NOT_FOUND -> throw VirusTotalException(VirusTotalErrorKind.NOT_FOUND, notFoundMessage)

                HTTP_TOO_MANY_REQUESTS -> throw VirusTotalException(
                    VirusTotalErrorKind.QUOTA_EXCEEDED,
                    "The VirusTotal API quota has been exceeded.",
                )

                else -> throw VirusTotalException(
                    VirusTotalErrorKind.UNKNOWN,
                    "VirusTotal returned HTTP ${response.code} $operation.",
                )
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: VirusTotalException) {
        throw error
    } catch (error: IOException) {
        currentCoroutineContext().ensureActive()
        Timber.w(error, "VirusTotal request failed")
        throw VirusTotalException(VirusTotalErrorKind.NETWORK_ERROR, "Could not reach VirusTotal.")
    }

    private fun validateUploadUrl(value: String?): String {
        val url = value?.toHttpUrlOrNull() ?: throw VirusTotalException(
            VirusTotalErrorKind.UNKNOWN,
            "VirusTotal returned an invalid upload URL.",
        )
        val trustedHost = url.host == apiBase.host ||
            (apiBase.host == "www.virustotal.com" && (url.host == "virustotal.com" || url.host.endsWith(".virustotal.com")))
        if (url.scheme != apiBase.scheme || !trustedHost || url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw VirusTotalException(VirusTotalErrorKind.UNKNOWN, "VirusTotal returned an untrusted upload URL.")
        }
        return url.toString()
    }

    private fun normalizeHash(value: String): String = value.trim().lowercase().also {
        if (!SHA256_PATTERN.matches(it)) {
            throw VirusTotalException(VirusTotalErrorKind.UNKNOWN, "A valid SHA-256 hash is required.")
        }
    }

    private inline fun <reified T> decode(body: String): T = try {
        json.decodeFromString<T>(body)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw VirusTotalException(VirusTotalErrorKind.UNKNOWN, "Could not parse the VirusTotal response.")
    }

    private fun parseReport(
        stats: VtStats,
        results: Map<String, VtEngineResult>?,
        sha256: String?,
        fileName: String?,
        fileSize: Long?,
        method: VirusTotalScanMethod,
        analysisId: String? = null,
        communityScores: VirusTotalCommunityScores? = null,
    ): VirusTotalReport {
        val positives = stats.malicious + stats.suspicious
        val total = stats.malicious + stats.suspicious + stats.undetected + stats.harmless + stats.timeout
        val detections = results.orEmpty().mapNotNull { (engineKey, result) ->
            if (result.category != "malicious" && result.category != "suspicious") return@mapNotNull null
            VirusTotalDetection(
                engine = result.engine_name?.takeIf { it.isNotBlank() } ?: engineKey,
                name = result.result?.trim()?.takeIf { it.isNotEmpty() },
            )
        }.distinctBy { it.engine.lowercase() }.sortedBy { it.engine.lowercase() }
        val likelyFalsePositive = VirusTotalFalsePositiveHeuristics.isLikelyFalsePositive(positives, total, detections)
        val verdict = when {
            positives <= 0 -> VirusTotalVerdict.CLEAN
            likelyFalsePositive -> VirusTotalVerdict.LIKELY_FALSE_POSITIVE
            stats.malicious > 0 -> VirusTotalVerdict.MALICIOUS
            else -> VirusTotalVerdict.SUSPICIOUS
        }
        return VirusTotalReport(
            verdict = verdict,
            positives = positives,
            total = total,
            detections = detections,
            permalink = sha256?.let { "$GUI_BASE_URL/file/$it" },
            sha256 = sha256,
            fileName = fileName,
            fileSize = fileSize,
            method = method,
            analysisId = analysisId,
            communityScores = communityScores,
        )
    }

    private companion object {
        const val BASE_URL = "https://www.virustotal.com/api/v3"
        const val GUI_BASE_URL = "https://www.virustotal.com/gui"
        const val API_KEY_HEADER = "x-apikey"
        const val UPLOAD_BUFFER_SIZE = 64 * 1024
        const val POLL_INTERVAL_MS = 3_000L
        const val MAX_ANALYSIS_WAIT_MS = 120_000L
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        val SHA256_PATTERN = Regex("^[a-f0-9]{64}$")
        val OCTET_STREAM: MediaType = "application/octet-stream".toMediaType()
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private suspend fun OkHttpClient.await(request: Request): Response = suspendCancellableCoroutine { continuation ->
    val call = newCall(request)
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, cancelledResponse, _ -> cancelledResponse.close() }
        }
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
    })
}

@Serializable
private data class VtUploadUrlResponse(val data: String? = null)

@Serializable
private data class VtFileResponse(val data: VtFileData? = null)

@Serializable
private data class VtFileData(val attributes: VtFileAttributes? = null)

@Serializable
private data class VtFileAttributes(
    val last_analysis_stats: VtStats? = null,
    val last_analysis_results: Map<String, VtEngineResult>? = null,
    val meaningful_name: String? = null,
    val size: Long? = null,
    val sha256: String? = null,
    val total_votes: VtCommunityVotes? = null,
    val reputation: Int = 0,
)

@Serializable
private data class VtCommunityVotes(val harmless: Int = 0, val malicious: Int = 0)

private fun VtFileAttributes.communityScores(): VirusTotalCommunityScores? = total_votes?.let {
    VirusTotalCommunityScores(it.harmless.coerceAtLeast(0), it.malicious.coerceAtLeast(0), reputation)
}

@Serializable
private data class VtAnalysisResponse(val data: VtAnalysisData? = null)

@Serializable
private data class VtAnalysisData(val id: String? = null, val attributes: VtAnalysisAttributes? = null)

@Serializable
private data class VtAnalysisAttributes(
    val status: String? = null,
    val stats: VtStats? = null,
    val results: Map<String, VtEngineResult>? = null,
)

@Serializable
private data class VtStats(
    val malicious: Int = 0,
    val suspicious: Int = 0,
    val undetected: Int = 0,
    val harmless: Int = 0,
    val timeout: Int = 0,
)

@Serializable
private data class VtEngineResult(val category: String? = null, val engine_name: String? = null, val result: String? = null)

@Serializable
private data class VtVoteRequest(val data: VtVoteData)

@Serializable
private data class VtVoteData(val attributes: VtVoteAttributes, val type: String = "vote")

@Serializable
private data class VtVoteAttributes(val verdict: String)

@Serializable
private data class VtVoteResponse(val data: VtVoteData? = null)
