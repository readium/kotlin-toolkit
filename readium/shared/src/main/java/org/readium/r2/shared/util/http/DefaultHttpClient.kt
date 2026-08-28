/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import android.os.Bundle
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.joinValues
import org.readium.r2.shared.extensions.lowerCaseKeys
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.http.HttpRequest.Method
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toDebugDescription
import org.readium.r2.shared.util.tryRecover
import timber.log.Timber

/**
 * An implementation of [HttpClient] using the native [HttpURLConnection].
 *
 * @param userAgent Custom user agent to use for requests.
 * @param connectTimeout Timeout used when establishing a connection to the resource. A null timeout
 *        is interpreted as the default value, while a timeout of zero as an infinite timeout.
 * @param readTimeout Timeout used when reading the input stream. A null timeout is interpreted
 *        as the default value, while a timeout of zero as an infinite timeout.
 */
public class DefaultHttpClient(
    private val userAgent: String? = null,
    private val connectTimeout: Duration? = null,
    private val readTimeout: Duration? = null,
    public var callback: Callback = object : Callback {},
) : HttpClient {

    public companion object {
        /**
         * [HttpRequest.extras] key for the number of redirections performed for a request.
         */
        private const val EXTRA_REDIRECT_COUNT: String = "redirectCount"
    }

    /**
     * Callbacks allowing to override some behavior of the [DefaultHttpClient].
     */
    public interface Callback {

        /**
         * Called when the HTTP client will start a new [request].
         *
         * You can modify the [request], for example by adding additional HTTP headers or
         * redirecting to a different URL, before returning it.
         */
        public suspend fun onStartRequest(request: HttpRequest): HttpTry<HttpRequest> =
            Try.success(request)

        /**
         * Called when the HTTP client received an [error] for the given [request], to provide an
         * opportunity to the implementer to recover from it.
         *
         * You can return either:
         *   - a new recovery request to start
         *   - the [error] argument, if you cannot recover from it
         *   - a new [HttpError] to provide additional information
         */
        public suspend fun onRecoverRequest(request: HttpRequest, error: HttpError): HttpTry<HttpRequest> =
            Try.failure(error)

        /**
         * Redirections are followed by default when the host and protocols are the same.
         * However, if for example an HTTP server redirects to an HTTPS URI, you will need to
         * confirm explicitly the redirection by implementing this callback as it is potentially
         * unsafe.
         *
         * It's recommended to confirm the redirection with the user, especially for a POST request.
         *
         * You can return either:
         *   - the provided [newRequest] to proceed with the redirection
         *   - a different redirection request
         */
        public suspend fun onFollowUnsafeRedirect(
            request: HttpRequest,
            response: HttpResponse,
            newRequest: HttpRequest,
        ): HttpTry<HttpRequest> =
            Try.failure(
                HttpError.Redirection(
                    DebugError("Request cancelled because of an unsafe redirect.")
                )
            )

        /**
         * Called when the HTTP client received an HTTP response for the given [request].
         *
         * You do not need to do anything with this [response], which the HTTP client will handle.
         * This is merely for informational purposes. For example, you could implement this to
         * confirm that request credentials were successful.
         */
        public suspend fun onResponseReceived(request: HttpRequest, response: HttpResponse) {}

        /**
         * Called when the HTTP client received an [error] for the given [request].
         *
         * You do not need to do anything with this `error`, which the HTTP client will handle. This
         * is merely for informational purposes.
         *
         * This will be called only if [onRecoverRequest] is not implemented, or returns an error.
         */
        public suspend fun onRequestFailed(request: HttpRequest, error: HttpError) {}
    }

    // We are using Dispatchers.IO but we still get this warning...
    override suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse> {
        suspend fun tryStream(request: HttpRequest): HttpTry<HttpStreamResponse> =
            withContext(Dispatchers.IO) {
                Timber.i("HTTP ${request.method.name} ${request.url}, headers: ${request.headers}")

                try {
                    var connection = request.toHttpURLConnection()

                    val statusCode = connection.responseCode
                    if (statusCode >= 400) {
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
                        val body = connection.errorStream?.use { it.readBytes() }

                        val mediaType = connection.contentType?.let { MediaType(it) }
                        return@withContext Try.failure(
                            HttpError.ErrorResponse(HttpStatus(statusCode), mediaType, body)
                        )
                    }

                    val response = HttpResponse(
                        request = request,
                        url = request.url,
                        statusCode = HttpStatus(statusCode),
                        headers = connection.safeHeaders,
                        mediaType = connection.contentType?.let { MediaType(it) }
                    )

                    callback.onResponseReceived(request, response)

                    if (statusCode in 300..399) {
                        followUnsafeRedirect(request, response)
                    } else {
                        Try.success(
                            HttpStreamResponse(
                                response = response,
                                body = HttpURLConnectionInputStream(connection)
                            )
                        )
                    }
                } catch (e: IOException) {
                    Try.failure(wrap(e))
                }
            }

        return callback.onStartRequest(request)
            .flatMap { tryStream(it) }
            .tryRecover { error ->
                callback.onRecoverRequest(request, error)
                    .flatMap { stream(it) }
            }
            .onFailure {
                callback.onRequestFailed(request, it)
                val error = DebugError("HTTP request failed ${request.url}", it)
                Timber.e(error.toDebugDescription())
            }
    }

    /**
     * HTTPUrlConnection follows by default redirections when the host and protocols are the same.
     *
     * However, if for example an HTTP server redirects to an HTTPS URI we need to handle the
     * redirection manually as it is considered unsafe. Apps will need to confirm explicitly the
     * redirection with [Callback.onFollowUnsafeRedirect].
     *
     * See https://bugs.openjdk.java.net/browse/JDK-4620571?focusedCommentId=12159233&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-12159233
     */
    private suspend fun followUnsafeRedirect(request: HttpRequest, response: HttpResponse): HttpTry<HttpStreamResponse> {
        // > A user agent should never automatically redirect a request more than 5 times, since
        // > such redirections usually indicate an infinite loop.
        // > https://www.rfc-editor.org/rfc/rfc1945.html#section-9.3
        val redirectCount = request.extras.getInt(EXTRA_REDIRECT_COUNT)
        if (redirectCount > 5) {
            return Try.failure(
                HttpError.Redirection(
                    DebugError("There were too many redirects to follow.")
                )
            )
        }

        val location = response.header("Location")
            ?.let { Url(it) }
            ?.let { request.url.resolve(it) }
            ?: return Try.failure(
                HttpError.MalformedResponse(
                    DebugError("Location of redirect is missing or invalid.")
                )
            )

        val newRequest = HttpRequest(
            url = location,
            body = request.body,
            method = request.method,
            headers = buildMap {
                response.headers("Set-Cookie")
                    .takeUnless { it.isEmpty() }
                    ?.let { put("Cookie", it) }
            },
            extras = Bundle().apply {
                putInt(EXTRA_REDIRECT_COUNT, redirectCount + 1)
            }
        )

        return callback
            .onFollowUnsafeRedirect(request, response = response, newRequest = newRequest)
            .flatMap { stream(it) }
    }

    private fun HttpRequest.toHttpURLConnection(): HttpURLConnection {
        val url = URL(url.toString())
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

        val normalizedHeaders = headers
            .lowerCaseKeys()
            .joinValues(",")

        for ((k, v) in normalizedHeaders) {
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

/**
 * Creates an HTTP error from a generic exception.
 */
private fun wrap(cause: IOException): HttpError =
    when (cause) {
        is UnknownHostException, is NoRouteToHostException, is ConnectException ->
            HttpError.Unreachable(ThrowableError(cause))
        is SocketTimeoutException ->
            HttpError.Timeout(ThrowableError(cause))
        is SSLHandshakeException ->
            HttpError.SslHandshake(ThrowableError(cause))
        else ->
            HttpError.IO(cause)
    }

/**
 * [HttpURLConnection]'s input stream which disconnects when closed.
 */
private class HttpURLConnectionInputStream(
    private val connection: HttpURLConnection,
) : InputStream() {

    private val inputStream = connection.inputStream

    override fun close() {
        super.close()
        connection.disconnect()
    }

    override fun read(): Int =
        inputStream.read()

    override fun read(b: ByteArray): Int =
        inputStream.read(b)

    override fun read(b: ByteArray, off: Int, len: Int): Int =
        inputStream.read(b, off, len)

    override fun skip(n: Long): Long =
        inputStream.skip(n)

    override fun available(): Int =
        inputStream.available()

    override fun mark(readlimit: Int) =
        inputStream.mark(readlimit)

    override fun reset() =
        inputStream.reset()

    override fun markSupported(): Boolean =
        inputStream.markSupported()
}
