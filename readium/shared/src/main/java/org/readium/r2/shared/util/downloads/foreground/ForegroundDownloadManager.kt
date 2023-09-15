/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads.foreground

import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpException
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpResponse
import org.readium.r2.shared.util.http.HttpTry

/**
 * A [DownloadManager] implementation using a [HttpClient].
 *
 * If the app is killed, downloads will stop and you won't be able to resume them later.
 */
public class ForegroundDownloadManager(
    private val httpClient: HttpClient,
    private val bufferLength: Int = 1024 * 8
) : DownloadManager {

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val jobs: MutableMap<DownloadManager.RequestId, Job> =
        mutableMapOf()

    private val listeners: MutableMap<DownloadManager.RequestId, MutableList<DownloadManager.Listener>> =
        mutableMapOf()

    public override fun submit(
        request: DownloadManager.Request,
        listener: DownloadManager.Listener
    ): DownloadManager.RequestId {
        val requestId = DownloadManager.RequestId(UUID.randomUUID().toString())
        register(requestId, listener)
        jobs[requestId] = coroutineScope.launch { doRequest(request, requestId) }
        return requestId
    }

    private suspend fun doRequest(request: DownloadManager.Request, id: DownloadManager.RequestId) {
        val destination = withContext(Dispatchers.IO) {
            File.createTempFile(UUID.randomUUID().toString(), null)
        }

        httpClient
            .download(
                request = HttpRequest(
                    url = request.url.toString(),
                    headers = request.headers.mapValues { it.value.joinToString(",") }
                ),
                destination = destination,
                onProgress = { downloaded, expected ->
                    forEachListener(id) {
                        onDownloadProgressed(id, downloaded = downloaded, expected = expected)
                    }
                }
            )
            .onSuccess { response ->
                forEachListener(id) {
                    onDownloadCompleted(
                        id,
                        file = destination,
                        mediaType = response.mediaType
                    )
                }
            }
            .onFailure { error ->
                forEachListener(id) {
                    onDownloadFailed(id, mapError(error))
                }
            }

        listeners.remove(id)
    }

    private fun forEachListener(
        id: DownloadManager.RequestId,
        task: DownloadManager.Listener.() -> Unit
    ) {
        listeners[id].orEmpty().forEach {
            tryOrLog { it.task() }
        }
    }

    public override fun cancel(requestId: DownloadManager.RequestId) {
        jobs.remove(requestId)?.cancel()
        forEachListener(requestId) { onDownloadCancelled(requestId) }
        listeners.remove(requestId)
    }

    public override fun register(
        requestId: DownloadManager.RequestId,
        listener: DownloadManager.Listener
    ) {
        listeners.getOrPut(requestId) { mutableListOf() }.add(listener)
    }

    public override fun close() {
        jobs.forEach { cancel(it.key) }
    }

    private suspend fun HttpClient.download(
        request: HttpRequest,
        destination: File,
        onProgress: (downloaded: Long, expected: Long?) -> Unit
    ): HttpTry<HttpResponse> =
        try {
            stream(request).flatMap { res ->
                withContext(Dispatchers.IO) {
                    val expected = res.response.contentLength?.takeIf { it > 0 }

                    res.body.use { input ->
                        FileOutputStream(destination).use { output ->
                            val buf = ByteArray(bufferLength)
                            var n: Int
                            var downloadedBytes = 0L
                            while (-1 != input.read(buf).also { n = it }) {
                                ensureActive()
                                downloadedBytes += n
                                output.write(buf, 0, n)
                                onProgress(downloadedBytes, expected)
                            }
                        }
                    }

                    Try.success(res.response)
                }
            }
        } catch (e: Exception) {
            Try.failure(HttpException.wrap(e))
        }

    private fun mapError(httpException: HttpException): DownloadManager.Error {
        val httpError = ThrowableError(httpException)
        return when (httpException.kind) {
            HttpException.Kind.MalformedRequest ->
                DownloadManager.Error.Unknown(httpError)

            HttpException.Kind.MalformedResponse ->
                DownloadManager.Error.HttpData(httpError)

            HttpException.Kind.Timeout ->
                DownloadManager.Error.Unreachable(httpError)

            HttpException.Kind.BadRequest ->
                DownloadManager.Error.Unknown(httpError)

            HttpException.Kind.Unauthorized ->
                DownloadManager.Error.Forbidden(httpError)

            HttpException.Kind.Forbidden ->
                DownloadManager.Error.Forbidden(httpError)

            HttpException.Kind.NotFound ->
                DownloadManager.Error.NotFound(httpError)

            HttpException.Kind.ClientError ->
                DownloadManager.Error.HttpData(httpError)

            HttpException.Kind.ServerError ->
                DownloadManager.Error.Server(httpError)

            HttpException.Kind.Offline ->
                DownloadManager.Error.Unreachable(httpError)

            HttpException.Kind.Cancelled ->
                DownloadManager.Error.Unknown(httpError)

            HttpException.Kind.Other ->
                DownloadManager.Error.Unknown(httpError)
        }
    }
}
