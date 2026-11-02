// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.virustotal

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.virustotal.VirusTotalScanner
import com.rosan.installer.domain.virustotal.calculateSha256
import com.rosan.installer.domain.virustotal.model.VirusTotalApiKeyFormat
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.model.VirusTotalReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanMethod
import com.rosan.installer.domain.virustotal.model.VirusTotalVerdict
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import timber.log.Timber

/**
 * VirusTotal API v3 client.
 *
 * The API key is sent in the `x-apikey` header and is never logged. Uploads are streamed from the
 * selected [DataEntity] so large APKs do not have to be copied into memory.
 */
class VirusTotalScannerImpl(
    private val client: OkHttpClient,
    private val json: Json,
) : VirusTotalScanner {

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
            val request = Request.Builder()
                .url("$BASE_URL/users/$apiKey/overall_quotas")
                .header(API_KEY_HEADER, apiKey)
                .build()

            client.newCall(request).execute().use { response ->
                when (response.code) {
                    HTTP_OK -> Result.success(Unit)

                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> Result.failure(
                        VirusTotalException(
                            kind = VirusTotalErrorKind.INVALID_API_KEY,
                            message = "VirusTotal rejected this API key.",
                        ),
                    )

                    HTTP_TOO_MANY_REQUESTS -> Result.failure(
                        VirusTotalException(
                            kind = VirusTotalErrorKind.QUOTA_EXCEEDED,
                            message = "The VirusTotal API quota has been exceeded.",
                        ),
                    )

                    else -> Result.failure(
                        VirusTotalException(
                            kind = VirusTotalErrorKind.UNKNOWN,
                            message = "VirusTotal returned HTTP ${response.code}.",
                        ),
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            Timber.w(error, "Could not reach VirusTotal to validate the API key")
            Result.failure(
                VirusTotalException(
                    kind = VirusTotalErrorKind.NETWORK_ERROR,
                    message = "Could not reach VirusTotal.",
                ),
            )
        }
    }

    override suspend fun lookupByHash(apiKey: String, sha256: String): VirusTotalReport = withContext(Dispatchers.IO) {
        val normalizedHash = sha256.trim().lowercase()
        val request = Request.Builder()
            .url("$BASE_URL/files/$normalizedHash")
            .header(API_KEY_HEADER, apiKey)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    HTTP_OK -> {
                        val body = response.body?.string()
                            ?: throw VirusTotalException(
                                VirusTotalErrorKind.UNKNOWN,
                                "VirusTotal returned an empty report.",
                            )
                        val parsed = decode<VtFileResponse>(body)
                        val attributes = parsed.data?.attributes
                        val stats = attributes?.last_analysis_stats
                        if (attributes == null || stats == null) {
                            throw VirusTotalException(
                                kind = VirusTotalErrorKind.NOT_FOUND,
                                message = "VirusTotal has no report for this hash yet.",
                            )
                        }
                        parseReport(
                            stats = stats,
                            results = attributes.last_analysis_results,
                            sha256 = attributes.sha256 ?: normalizedHash,
                            fileName = attributes.meaningful_name,
                            fileSize = attributes.size,
                            method = VirusTotalScanMethod.HASH,
                        )
                    }

                    HTTP_NOT_FOUND -> throw VirusTotalException(
                        kind = VirusTotalErrorKind.NOT_FOUND,
                        message = "VirusTotal has no report for this hash yet.",
                    )

                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> throw VirusTotalException(
                        kind = VirusTotalErrorKind.INVALID_API_KEY,
                        message = "VirusTotal rejected this API key.",
                    )

                    HTTP_TOO_MANY_REQUESTS -> throw VirusTotalException(
                        kind = VirusTotalErrorKind.QUOTA_EXCEEDED,
                        message = "The VirusTotal API quota has been exceeded.",
                    )

                    else -> throw VirusTotalException(
                        kind = VirusTotalErrorKind.UNKNOWN,
                        message = "VirusTotal returned HTTP ${response.code}.",
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            throw error
        } catch (error: IOException) {
            Timber.w(error, "Could not reach VirusTotal for hash lookup")
            throw VirusTotalException(
                kind = VirusTotalErrorKind.NETWORK_ERROR,
                message = "Could not reach VirusTotal.",
            )
        }
    }

    override suspend fun uploadAndScan(
        apiKey: String,
        data: DataEntity,
        fileName: String,
    ): VirusTotalReport = withContext(Dispatchers.IO) {
        val size = data.getSize()
        if (size > MAX_UPLOAD_BYTES) {
            throw VirusTotalException(
                kind = VirusTotalErrorKind.FILE_TOO_LARGE,
                message = "The file is larger than the 32 MB upload limit.",
            )
        }

        // Compute the hash up front so the report has a stable permalink even if the analysis
        // response does not include it.
        val sha256 = data.calculateSha256()

        val uploadBody = object : RequestBody() {
            override fun contentType(): MediaType = OCTET_STREAM

            override fun contentLength(): Long = if (size >= 0) size else -1L

            override fun writeTo(sink: BufferedSink) {
                val input = data.getInputStream()
                    ?: throw IOException("The selected file could not be opened for upload.")
                input.use { stream ->
                    val buffer = ByteArray(UPLOAD_BUFFER_SIZE)
                    while (true) {
                        val bytesRead = stream.read(buffer)
                        if (bytesRead < 0) break
                        sink.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, uploadBody)
            .build()

        val uploadRequest = Request.Builder()
            .url("$BASE_URL/files")
            .header(API_KEY_HEADER, apiKey)
            .post(multipart)
            .build()

        val analysisId = try {
            client.newCall(uploadRequest).execute().use { response ->
                when (response.code) {
                    HTTP_OK -> {
                        val body = response.body?.string()
                            ?: throw VirusTotalException(
                                VirusTotalErrorKind.UNKNOWN,
                                "VirusTotal returned an empty upload response.",
                            )
                        decode<VtAnalysisResponse>(body).data?.id
                            ?: throw VirusTotalException(
                                VirusTotalErrorKind.UNKNOWN,
                                "VirusTotal did not return an analysis id.",
                            )
                    }

                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> throw VirusTotalException(
                        kind = VirusTotalErrorKind.INVALID_API_KEY,
                        message = "VirusTotal rejected this API key.",
                    )

                    HTTP_TOO_MANY_REQUESTS -> throw VirusTotalException(
                        kind = VirusTotalErrorKind.QUOTA_EXCEEDED,
                        message = "The VirusTotal API quota has been exceeded.",
                    )

                    else -> throw VirusTotalException(
                        kind = VirusTotalErrorKind.UNKNOWN,
                        message = "VirusTotal returned HTTP ${response.code} while uploading.",
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            throw error
        } catch (error: IOException) {
            Timber.w(error, "Could not upload the file to VirusTotal")
            throw VirusTotalException(
                kind = VirusTotalErrorKind.NETWORK_ERROR,
                message = "Could not reach VirusTotal.",
            )
        }

        val deadline = System.currentTimeMillis() + MAX_ANALYSIS_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            delay(POLL_INTERVAL_MS)

            val analysisRequest = Request.Builder()
                .url("$BASE_URL/analyses/$analysisId")
                .header(API_KEY_HEADER, apiKey)
                .build()

            val analysis = try {
                client.newCall(analysisRequest).execute().use { response ->
                    when (response.code) {
                        HTTP_OK -> {
                            val body = response.body?.string()
                                ?: throw VirusTotalException(
                                    VirusTotalErrorKind.UNKNOWN,
                                    "VirusTotal returned an empty analysis.",
                                )
                            decode<VtAnalysisResponse>(body)
                        }

                        HTTP_NOT_FOUND -> throw VirusTotalException(
                            kind = VirusTotalErrorKind.UNKNOWN,
                            message = "The VirusTotal analysis could not be found.",
                        )

                        HTTP_TOO_MANY_REQUESTS -> throw VirusTotalException(
                            kind = VirusTotalErrorKind.QUOTA_EXCEEDED,
                            message = "The VirusTotal API quota has been exceeded.",
                        )

                        else -> throw VirusTotalException(
                            kind = VirusTotalErrorKind.UNKNOWN,
                            message = "VirusTotal returned HTTP ${response.code} while polling.",
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: VirusTotalException) {
                throw error
            } catch (error: IOException) {
                Timber.w(error, "Could not poll the VirusTotal analysis")
                throw VirusTotalException(
                    kind = VirusTotalErrorKind.NETWORK_ERROR,
                    message = "Could not reach VirusTotal.",
                )
            }

            val attributes = analysis.data?.attributes
            when (attributes?.status) {
                "completed" -> {
                    val stats = attributes?.stats
                        ?: throw VirusTotalException(
                            kind = VirusTotalErrorKind.UNKNOWN,
                            message = "VirusTotal completed the analysis without statistics.",
                        )
                    return@withContext parseReport(
                        stats = stats,
                        results = attributes?.results,
                        sha256 = sha256,
                        fileName = fileName,
                        fileSize = if (size >= 0) size else null,
                        method = VirusTotalScanMethod.UPLOAD,
                        analysisId = analysisId,
                    )
                }

                "failed" -> throw VirusTotalException(
                    kind = VirusTotalErrorKind.UNKNOWN,
                    message = "VirusTotal failed to analyze this file.",
                )

                else -> Unit
            }
        }

        throw VirusTotalException(
            kind = VirusTotalErrorKind.TIMEOUT,
            message = "The VirusTotal analysis did not finish in time.",
        )
    }

    private inline fun <reified T> decode(body: String): T = try {
        json.decodeFromString<T>(body)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw VirusTotalException(
            kind = VirusTotalErrorKind.UNKNOWN,
            message = "Could not parse the VirusTotal response.",
        )
    }

    private fun parseReport(
        stats: VtStats,
        results: Map<String, VtEngineResult>?,
        sha256: String?,
        fileName: String?,
        fileSize: Long?,
        method: VirusTotalScanMethod,
        analysisId: String? = null,
    ): VirusTotalReport {
        val positives = stats.malicious + stats.suspicious
        val total = stats.malicious +
            stats.suspicious +
            stats.undetected +
            stats.harmless +
            stats.timeout
        val verdict = when {
            stats.malicious > 0 -> VirusTotalVerdict.MALICIOUS
            stats.suspicious > 0 -> VirusTotalVerdict.SUSPICIOUS
            else -> VirusTotalVerdict.CLEAN
        }
        val detections = results
            ?.values
            ?.filter { result ->
                result.category == "malicious" || result.category == "suspicious"
            }
            ?.mapNotNull { result -> result.engine_name?.takeIf { it.isNotBlank() } }
            ?.distinct()
            ?.sorted()
            .orEmpty()

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
        )
    }

    private companion object {
        const val BASE_URL = "https://www.virustotal.com/api/v3"
        const val GUI_BASE_URL = "https://www.virustotal.com/gui"
        const val API_KEY_HEADER = "x-apikey"
        const val MAX_UPLOAD_BYTES = 32L * 1024L * 1024L
        const val UPLOAD_BUFFER_SIZE = 64 * 1024
        const val POLL_INTERVAL_MS = 3_000L
        const val MAX_ANALYSIS_WAIT_MS = 120_000L
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        val OCTET_STREAM: MediaType = "application/octet-stream".toMediaType()
    }
}

@Serializable
private data class VtFileResponse(
    val data: VtFileData? = null,
)

@Serializable
private data class VtFileData(
    val id: String? = null,
    val attributes: VtFileAttributes? = null,
)

@Serializable
private data class VtFileAttributes(
    val last_analysis_stats: VtStats? = null,
    val last_analysis_results: Map<String, VtEngineResult>? = null,
    val meaningful_name: String? = null,
    val size: Long? = null,
    val sha256: String? = null,
)

@Serializable
private data class VtAnalysisResponse(
    val data: VtAnalysisData? = null,
)

@Serializable
private data class VtAnalysisData(
    val id: String? = null,
    val attributes: VtAnalysisAttributes? = null,
)

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
private data class VtEngineResult(
    val category: String? = null,
    val engine_name: String? = null,
    val result: String? = null,
)
