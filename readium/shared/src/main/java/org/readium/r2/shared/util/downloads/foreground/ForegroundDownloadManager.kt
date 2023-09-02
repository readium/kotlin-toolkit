/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads.foreground

import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpException
import org.readium.r2.shared.util.http.HttpRequest

public class ForegroundDownloadManager(
    private val httpClient: HttpClient,
    private val listener: DownloadManager.Listener
) : DownloadManager {

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val jobs: MutableMap<DownloadManager.RequestId, Job> =
        mutableMapOf()

    override suspend fun submit(request: DownloadManager.Request): DownloadManager.RequestId {
        val requestId = DownloadManager.RequestId(UUID.randomUUID().toString())
        jobs[requestId] = coroutineScope.launch { doRequest(request, requestId) }
        return requestId
    }

    private suspend fun doRequest(request: DownloadManager.Request, id: DownloadManager.RequestId) {
        val response = httpClient.fetch(
            HttpRequest(
                url = request.url.toString(),
                headers = request.headers.mapValues { it.value.joinToString(",") }
            )
        )

        val dottedExtension = request.url.extension
            ?.let { ".$it" }
            .orEmpty()

        when (response) {
            is Try.Success -> {
                withContext(Dispatchers.IO) {
                    try {
                        val dest = File.createTempFile(
                            UUID.randomUUID().toString(),
                            dottedExtension
                        )
                        dest.writeBytes(response.value.body)
                    } catch (e: Exception) {
                        val error = DownloadManager.Error.FileError(ThrowableError(e))
                        listener.onDownloadFailed(id, error)
                    }
                }
            }
            is Try.Failure -> {
                val error = mapError(response.value)
                listener.onDownloadFailed(id, error)
            }
        }
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

    override suspend fun cancel(requestId: DownloadManager.RequestId) {
        jobs.remove(requestId)?.cancel()
    }

    override suspend fun close() {
    }
}
