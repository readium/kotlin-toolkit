/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.http.HttpRequest.Method
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.sniffMediaType
import org.readium.r2.shared.util.tryRecover
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration

/**
 * An implementation of [HttpClient] using the native [HttpURLConnection].
 *
 * @param userAgent Custom user agent to use for requests.
 * @param additionalHeaders A dictionary of additional headers to send with requests.
 * @param connectTimeout Timeout used when establishing a connection to the resource. A null timeout
 *        is interpreted as the default value, while a timeout of zero as an infinite timeout.
 * @param readTimeout Timeout used when reading the input stream. A null timeout is interpreted
 *        as the default value, while a timeout of zero as an infinite timeout.
 */
class DefaultHttpClient constructor(
    private val userAgent: String? = null,
    private val additionalHeaders: Map<String, String> = mapOf(),
    private val connectTimeout: Duration? = null,
    private val readTimeout: Duration? = null,
    var callback: Callback? = null,
) : HttpClient {

    /**
     * Callbacks allowing to override some behavior of the [DefaultHttpClient].
     */
    interface Callback {

        /**
         * Called when the HTTP client will start a new [request].
         *
         * You can modify the [request], for example by adding additional HTTP headers or
         * redirecting to a different URL, before returning it.
         */
        suspend fun onStartRequest(request: HttpRequest): HttpTry<HttpRequest> =
            Try.success(request)

        /**
         * Called when the HTTP client received an [error] for the given [request], to provide an
         * opportunity to the implementer to recover from it.
         *
         * You can return either:
         *   - a new recovery request to start
         *   - the [error] argument, if you cannot recover from it
         *   - a new [HttpException] to provide additional information
         */
        suspend fun onRecoverRequest(request: HttpRequest, error: HttpException): HttpTry<HttpRequest> =
            Try.failure(error)

        /**
         * Called when the HTTP client received an HTTP response for the given [request].
         *
         * You do not need to do anything with this [response], which the HTTP client will handle.
         * This is merely for informational purposes. For example, you could implement this to
         * confirm that request credentials were successful.
         */
        suspend fun onResponseReceived(request: HttpRequest, response: HttpResponse) {}

        /**
         * Called when the HTTP client received an [error] for the given [request].
         *
         * You do not need to do anything with this `error`, which the HTTP client will handle. This
         * is merely for informational purposes.
         *
         * This will be called only if [onRecoverRequest] is not implemented, or returns an error.
         */
        suspend fun onRequestFailed(request: HttpRequest, error: HttpException) {}

    }

    // We are using Dispatchers.IO but we still get this warning...
    @Suppress("BlockingMethodInNonBlockingContext", "NAME_SHADOWING")
    override suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse> {

        suspend fun tryStream(request: HttpRequest): HttpTry<HttpStreamResponse> =
            withContext(Dispatchers.IO) {
                Timber.i("HTTP ${request.method.name} ${request.url}, headers: ${request.headers}")

                try {
                    var connection = request.toHttpURLConnection()

                    val statusCode = connection.responseCode
                    HttpException.Kind.ofStatusCode(statusCode)?.let { kind ->
                        // It was a HEAD request? We need to query the resource again to get the error body.
                        // The body is needed for example when the response is an OPDS Authentication
                        // Document.
                        if (request.method == Method.HEAD) {
                            connection = request
                                .buildUpon()
                                .apply { method = Method.GET }
                                .build()
                                .toHttpURLConnection()
                        }

                        // Reads the full body, since it might contain an error representation such as
                        // JSON Problem Details or OPDS Authentication Document
                        val body = connection.errorStream.use { it.readBytes() }
                        val mediaType = connection.sniffMediaType(bytes = { body })
                        throw HttpException(kind, mediaType, body)
                    }

                    val response = HttpResponse(
                        request = request,
                        url = connection.url.toString(),
                        statusCode = statusCode,
                        headers = connection.safeHeaders,
                        mediaType = connection.sniffMediaType() ?: MediaType.BINARY,
                    )

                    callback?.onResponseReceived(request, response)
                    Try.success(HttpStreamResponse(
                        response = response,
                        body = connection.inputStream,
                    ))

                } catch (e: Exception) {
                    Try.failure(HttpException.wrap(e))
                }
            }


        return onStartRequest(request)
            .flatMap { tryStream(it) }
            .tryRecover { error ->
                if (error.kind != HttpException.Kind.Cancelled) {
                    onRecoverRequest(request, error)
                        .flatMap { stream(it) }
                } else {
                    Try.failure(error)
                }
            }
            .onFailure {
                callback?.onRequestFailed(request, it)
                Timber.e(it, "HTTP request failed ${request.url}")
            }
    }

    private suspend fun onStartRequest(request: HttpRequest): HttpTry<HttpRequest> =
        callback?.onStartRequest(request) ?: Try.success(request)

    private suspend fun onRecoverRequest(request: HttpRequest, error: HttpException): HttpTry<HttpRequest> =
        callback?.onRecoverRequest(request, error) ?: Try.failure(error)

    private fun HttpRequest.toHttpURLConnection(): HttpURLConnection {
        val url = URL(url)
        val connection = (url.openConnection() as HttpURLConnection)
        connection.requestMethod = method.name

        val readTimeout = readTimeout ?: this@DefaultHttpClient.readTimeout
        if (readTimeout != null) {
            connection.readTimeout = readTimeout.inWholeMilliseconds.toInt()
        }

        val connectTimeout = connectTimeout ?: this@DefaultHttpClient.connectTimeout
        if (connectTimeout != null) {
            connection.connectTimeout = connectTimeout.inWholeMilliseconds.toInt()
        }
        connection.allowUserInteraction = allowUserInteraction

        if (userAgent != null) {
            connection.setRequestProperty("User-Agent", userAgent)
        }

        for ((k, v) in this@DefaultHttpClient.additionalHeaders) {
            connection.setRequestProperty(k, v)
        }
        for ((k, v) in headers) {
            connection.setRequestProperty(k, v)
        }

        if (body != null) {
            connection.doOutput = true

            connection.outputStream.use { outputStream ->
                val inputStream = when (body) {
                    is HttpRequest.Body.Bytes ->
                        ByteArrayInputStream(body.bytes)
                    is HttpRequest.Body.File ->
                        FileInputStream(body.file)
                }

                inputStream.use { it.copyTo(outputStream) }
            }
        }

        return connection
    }

    private val HttpURLConnection.safeHeaders: Map<String, List<String>> get() =
        headerFields.filterNot { (key, value) ->
            // In practice, I found that some header names are null despite the force unwrapping.
            @Suppress("SENSELESS_COMPARISON")
            key == null || value == null
        }

}
