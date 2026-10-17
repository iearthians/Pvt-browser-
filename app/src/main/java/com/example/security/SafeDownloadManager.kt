package com.example.security

import com.example.model.DownloadLifecycleState
import com.example.model.DownloadTaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SafeDownloadManager(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
) {
    private val activeCalls = ConcurrentHashMap<String, Call>()

    companion object {
        const val MAX_IN_MEMORY_DOWNLOAD_BYTES = 15L * 1024L * 1024L // 15 MB cap for ephemeral RAM safety
    }

    fun cancelDownload(taskId: String) {
        activeCalls.remove(taskId)?.cancel()
    }

    fun cancelAllDownloads() {
        val keys = activeCalls.keys().toList()
        for (k in keys) {
            activeCalls.remove(k)?.cancel()
        }
    }

    suspend fun executeSafeDownload(
        taskId: String,
        initialUrl: String,
        maxRedirects: Int = 5,
        userConfirmedExecutable: Boolean = false,
        onUpdate: (DownloadTaskItem) -> Unit
    ): DownloadTaskItem = withContext(Dispatchers.IO) {
        val initialValidation = UrlSecurityValidator.validateDirectUrl(
            rawUrl = initialUrl,
            enforceHttpsOnly = true,
            blockPrivateNetworkSsrf = true,
            stripTrackingParams = true
        )

        if (initialValidation is UrlValidationResult.Rejected) {
            val failed = DownloadTaskItem(
                id = taskId,
                sourceUrl = initialUrl,
                finalResolvedUrl = initialUrl,
                sanitizedFileName = "rejected_download",
                mimeType = "unknown",
                totalBytes = 0L,
                downloadedBytes = 0L,
                state = DownloadLifecycleState.FAILED,
                isExecutableOrDangerous = false,
                errorMessage = "${initialValidation.reasonTitle}: ${initialValidation.technicalDetail}"
            )
            onUpdate(failed)
            return@withContext failed
        }

        var currentUrl = (initialValidation as UrlValidationResult.ValidDirectUrl).normalizedUrl
        val redirectChain = mutableListOf(currentUrl)

        val InitialClassification = UrlSecurityValidator.classifyAndSanitizeDownload(
            rawFileNameOrUrl = currentUrl,
            contentTypeHeader = null
        )

        if (InitialClassification.isExecutableOrDangerous && !userConfirmedExecutable) {
            val awaitingConfirm = DownloadTaskItem(
                id = taskId,
                sourceUrl = initialUrl,
                finalResolvedUrl = currentUrl,
                sanitizedFileName = InitialClassification.sanitizedFileName,
                mimeType = "application/octet-stream (flagged pre-flight)",
                totalBytes = -1L,
                downloadedBytes = 0L,
                state = DownloadLifecycleState.AWAITING_EXECUTABLE_CONFIRMATION,
                isExecutableOrDangerous = true,
                dangerWarningReason = InitialClassification.warningReason,
                redirectChain = redirectChain.toList()
            )
            onUpdate(awaitingConfirm)
            return@withContext awaitingConfirm
        }

        return@withContext try {
            for (hop in 0..maxRedirects) {
                coroutineContext.ensureActive()

                val request = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", "GhostSearch-Ephemeral/1.0 (Privacy-First; +https-only)")
                    .header("Accept", "*/*")
                    .get()
                    .build()

                val call = httpClient.newCall(request)
                activeCalls[taskId] = call

                val response = call.execute()
                if (response.code in listOf(301, 302, 303, 307, 308)) {
                    val locationHeader = response.header("Location")
                    response.close()
                    if (locationHeader.isNullOrBlank()) {
                        return@withContext failTask(
                            taskId,
                            initialUrl,
                            currentUrl,
                            InitialClassification.sanitizedFileName,
                            redirectChain,
                            "Redirect response (${response.code}) missing Location header.",
                            onUpdate
                        )
                    }
                    if (hop + 1 > maxRedirects) {
                        return@withContext failTask(
                            taskId,
                            initialUrl,
                            currentUrl,
                            InitialClassification.sanitizedFileName,
                            redirectChain,
                            "Exceeded maximum safe redirect hops ($maxRedirects).",
                            onUpdate
                        )
                    }

                    val resolvedNextUrl = response.request.url.resolve(locationHeader)?.toString()
                        ?: locationHeader
                    val hopValidation = UrlSecurityValidator.validateDirectUrl(
                        rawUrl = resolvedNextUrl,
                        enforceHttpsOnly = true,
                        blockPrivateNetworkSsrf = true,
                        stripTrackingParams = true
                    )
                    if (hopValidation is UrlValidationResult.Rejected) {
                        return@withContext failTask(
                            taskId,
                            initialUrl,
                            resolvedNextUrl,
                            InitialClassification.sanitizedFileName,
                            redirectChain,
                            "Unsafe redirect blocked (${hopValidation.reasonTitle}): ${hopValidation.technicalDetail}",
                            onUpdate
                        )
                    }
                    currentUrl = (hopValidation as UrlValidationResult.ValidDirectUrl).normalizedUrl
                    redirectChain.add(currentUrl)
                    continue
                }

                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    return@withContext failTask(
                        taskId,
                        initialUrl,
                        currentUrl,
                        InitialClassification.sanitizedFileName,
                        redirectChain,
                        "HTTP $code — Server rejected request or requires authentication/paywall access.",
                        onUpdate
                    )
                }

                val contentType = response.header("Content-Type")?.substringBefore(';')?.trim()
                    ?: "application/octet-stream"
                val contentDisposition = response.header("Content-Disposition")
                val filenameCandidate = extractDispositionFileName(contentDisposition) ?: currentUrl
                val finalSafety = UrlSecurityValidator.classifyAndSanitizeDownload(
                    rawFileNameOrUrl = filenameCandidate,
                    contentTypeHeader = contentType
                )

                if (finalSafety.isExecutableOrDangerous && !userConfirmedExecutable) {
                    response.close()
                    val awaiting = DownloadTaskItem(
                        id = taskId,
                        sourceUrl = initialUrl,
                        finalResolvedUrl = currentUrl,
                        sanitizedFileName = finalSafety.sanitizedFileName,
                        mimeType = contentType,
                        totalBytes = response.body?.contentLength() ?: -1L,
                        downloadedBytes = 0L,
                        state = DownloadLifecycleState.AWAITING_EXECUTABLE_CONFIRMATION,
                        isExecutableOrDangerous = true,
                        dangerWarningReason = finalSafety.warningReason,
                        redirectChain = redirectChain.toList()
                    )
                    onUpdate(awaiting)
                    return@withContext awaiting
                }

                val body = response.body ?: run {
                    response.close()
                    return@withContext failTask(
                        taskId,
                        initialUrl,
                        currentUrl,
                        finalSafety.sanitizedFileName,
                        redirectChain,
                        "Empty response body from server.",
                        onUpdate
                    )
                }

                val contentLength = body.contentLength()
                if (contentLength > MAX_IN_MEMORY_DOWNLOAD_BYTES) {
                    response.close()
                    return@withContext failTask(
                        taskId,
                        initialUrl,
                        currentUrl,
                        finalSafety.sanitizedFileName,
                        redirectChain,
                        "File exceeds 15 MB ephemeral RAM safety ceiling (${contentLength / (1024 * 1024)} MB).",
                        onUpdate
                    )
                }

                val digest = MessageDigest.getInstance("SHA-256")
                val outputBuffer = ByteArrayOutputStream()
                val chunk = ByteArray(8 * 1024)
                var totalRead = 0L

                body.byteStream().use { input ->
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(chunk)
                        if (read == -1) break
                        totalRead += read
                        if (totalRead > MAX_IN_MEMORY_DOWNLOAD_BYTES) {
                            response.close()
                            return@withContext failTask(
                                taskId,
                                initialUrl,
                                currentUrl,
                                finalSafety.sanitizedFileName,
                                redirectChain,
                                "Download stream exceeded 15 MB ephemeral RAM ceiling; aborted.",
                                onUpdate
                            )
                        }
                        digest.update(chunk, 0, read)
                        outputBuffer.write(chunk, 0, read)

                        onUpdate(
                            DownloadTaskItem(
                                id = taskId,
                                sourceUrl = initialUrl,
                                finalResolvedUrl = currentUrl,
                                sanitizedFileName = finalSafety.sanitizedFileName,
                                mimeType = contentType,
                                totalBytes = contentLength,
                                downloadedBytes = totalRead,
                                state = DownloadLifecycleState.DOWNLOADING,
                                isExecutableOrDangerous = finalSafety.isExecutableOrDangerous,
                                dangerWarningReason = finalSafety.warningReason,
                                redirectChain = redirectChain.toList()
                            )
                        )
                    }
                }

                val sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }
                val completedItem = DownloadTaskItem(
                    id = taskId,
                    sourceUrl = initialUrl,
                    finalResolvedUrl = currentUrl,
                    sanitizedFileName = finalSafety.sanitizedFileName,
                    mimeType = contentType,
                    totalBytes = if (contentLength > 0) contentLength else totalRead,
                    downloadedBytes = totalRead,
                    state = DownloadLifecycleState.COMPLETED,
                    isExecutableOrDangerous = finalSafety.isExecutableOrDangerous,
                    dangerWarningReason = finalSafety.warningReason,
                    redirectChain = redirectChain.toList(),
                    sha256Hex = sha256Hex,
                    savedMemoryBuffer = outputBuffer.toByteArray()
                )
                activeCalls.remove(taskId)
                onUpdate(completedItem)
                return@withContext completedItem
            }
            failTask(
                taskId,
                initialUrl,
                currentUrl,
                InitialClassification.sanitizedFileName,
                redirectChain,
                "Exceeded maximum safe redirect hops ($maxRedirects).",
                onUpdate
            )
        } catch (e: Exception) {
            activeCalls.remove(taskId)
            val isCancelled = e is java.io.IOException && e.message?.contains("Canceled", ignoreCase = true) == true ||
                e is kotlinx.coroutines.CancellationException
            val finalState = if (isCancelled) DownloadLifecycleState.CANCELLED else DownloadLifecycleState.FAILED
            val item = DownloadTaskItem(
                id = taskId,
                sourceUrl = initialUrl,
                finalResolvedUrl = currentUrl,
                sanitizedFileName = InitialClassification.sanitizedFileName,
                mimeType = "unknown",
                totalBytes = 0L,
                downloadedBytes = 0L,
                state = finalState,
                isExecutableOrDangerous = InitialClassification.isExecutableOrDangerous,
                redirectChain = redirectChain.toList(),
                errorMessage = if (isCancelled) "Download cancelled by user; volatile buffer wiped." else (e.message ?: "Network error during HTTPS download.")
            )
            onUpdate(item)
            item
        }
    }

    private fun extractDispositionFileName(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        val match = Regex("filename\\*?=['\"]?([^'\";]+)['\"]?", RegexOption.IGNORE_CASE)
            .find(disposition)
        return match?.groupValues?.getOrNull(1)?.trim()
    }

    private fun failTask(
        taskId: String,
        sourceUrl: String,
        resolvedUrl: String,
        sanitizedFileName: String,
        redirectChain: List<String>,
        reason: String,
        onUpdate: (DownloadTaskItem) -> Unit
    ): DownloadTaskItem {
        activeCalls.remove(taskId)
        val failed = DownloadTaskItem(
            id = taskId,
            sourceUrl = sourceUrl,
            finalResolvedUrl = resolvedUrl,
            sanitizedFileName = sanitizedFileName,
            mimeType = "unknown",
            totalBytes = 0L,
            downloadedBytes = 0L,
            state = DownloadLifecycleState.FAILED,
            isExecutableOrDangerous = false,
            redirectChain = redirectChain.toList(),
            errorMessage = reason
        )
        onUpdate(failed)
        return failed
    }
}
