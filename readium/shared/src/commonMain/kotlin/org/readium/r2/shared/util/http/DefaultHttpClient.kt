/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import io.ktor.client.HttpClient as KtorHttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse as KtorHttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.util.toMap
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.joinValues
import org.readium.r2.shared.extensions.lowerCaseKeys
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.join
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.file.fileSystem
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.http.HttpRequest.Method
import org.readium.r2.shared.util.logging.ReadiumLog
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toDebugDescription
import org.readium.r2.shared.util.tryRecover

/**
 * The HTTP engine used by [DefaultHttpClient]: OkHttp on Android, Darwin (NSURLSession) on iOS.
 */
internal expect fun defaultHttpClientEngine(): HttpClientEngine

/**
 * An implementation of [HttpClient] backed by [Ktor](https://ktor.io) (OkHttp engine on Android,
 * Darwin engine on iOS).
 *
 * The client holds a network engine and a coroutine scope hosting the in-flight requests. If you
 * use a short-lived instance, call [close] when you are done with it to release these resources
 * and terminate any request still streaming its response body. A closed client cannot perform
 * any new request.
 *
 * @param userAgent Custom user agent to use for requests.
 * @param connectTimeout Timeout used when establishing a connection to the resource. A null timeout
 *        is interpreted as the default value, while a timeout of zero as an infinite timeout.
 * @param readTimeout Timeout used when reading the input stream. A null timeout is interpreted
 *        as the default value, while a timeout of zero as an infinite timeout.
 */
public class DefaultHttpClient internal constructor(
    private val userAgent: String? = null,
    private val connectTimeout: Duration? = null,
    private val readTimeout: Duration? = null,
    public var callback: Callback = object : Callback {},
    engine: HttpClientEngine? = null,
) : HttpClient, Closeable {

    public constructor(
        userAgent: String? = null,
        connectTimeout: Duration? = null,
        readTimeout: Duration? = null,
        callback: Callback = object : Callback {},
    ) : this(userAgent, connectTimeout, readTimeout, callback, engine = null)

    public companion object {
        /**
         * [HttpRequest.extras] key for the number of redirections performed for a request.
         */
        private const val EXTRA_REDIRECT_COUNT: String = "redirectCount"

        /**
         * Maximum number of redirections followed for a single request.
         *
         * > A user agent should never automatically redirect a request more than 5 times, since
         * > such redirections usually indicate an infinite loop.
         * > https://www.rfc-editor.org/rfc/rfc1945.html#section-9.3
         */
        private const val MAX_REDIRECTS: Int = 5

        /**
         * Status codes triggering a redirection. The other 3xx codes (e.g. 304 Not Modified) are
         * regular responses.
         */
        private val REDIRECT_STATUS_CODES: Set<Int> = setOf(301, 302, 303, 307, 308)

        /**
         * Headers carrying credentials, which must not be forwarded to a different origin.
         */
        private val CREDENTIAL_HEADERS: Set<String> =
            setOf("authorization", "proxy-authorization", "cookie")
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
         * Redirections are followed by default when the protocols are the same.
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

    private val client: KtorHttpClient =
        KtorHttpClient(engine ?: defaultHttpClientEngine()) {
            followRedirects = false
            expectSuccess = false
            install(HttpTimeout)
        }

    /**
     * Scope hosting the in-flight requests, so that a streamed response body can outlive the
     * suspending call to [stream].
     */
    private val requestScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Cancels the in-flight requests (including response bodies still being streamed) and
     * releases the underlying network engine.
     *
     * Any request performed after closing the client fails with an [HttpError].
     */
    override fun close() {
        requestScope.cancel()
        client.close()
    }

    override suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse> {
        suspend fun tryStream(request: HttpRequest): HttpTry<HttpStreamResponse> {
            ReadiumLog.d(
                "HTTP ${request.method.name} ${request.url}, headers: ${request.headers.redacted()}"
            )

            return execute(request).flatMap { outcome ->
                when (outcome) {
                    is Outcome.Response ->
                        Try.success(outcome.response)
                    is Outcome.Redirect ->
                        followRedirect(request, outcome.response)
                }
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
                ReadiumLog.e(error.toDebugDescription())
            }
    }

    private sealed class Outcome {
        class Response(val response: HttpStreamResponse) : Outcome()
        class Redirect(val response: HttpResponse) : Outcome()
    }

    /**
     * Executes a single HTTP exchange, without following redirections.
     */
    private suspend fun execute(request: HttpRequest): HttpTry<Outcome> {
        val result = executeOnce(request)

        // It was a HEAD request which failed? We need to query the resource again with GET to
        // fetch the error body, needed for example when the response is an OPDS Authentication
        // Document.
        val error = result.failureOrNull()
        if (request.method == Method.HEAD && error is HttpError.ErrorResponse) {
            val getResult = executeOnce(request.copy { method = Method.GET })

            // If the GET request unexpectedly succeeds, discard its streamed body to release
            // the connection.
            (getResult.getOrNull() as? Outcome.Response)
                ?.response?.body?.close()

            val getError = getResult.failureOrNull()
            if (getError is HttpError.ErrorResponse) {
                return Try.failure(
                    HttpError.ErrorResponse(error.status, getError.mediaType, getError.body)
                )
            }
        }

        return result
    }

    private suspend fun executeOnce(request: HttpRequest): HttpTry<Outcome> {
        val responseReady = CompletableDeferred<HttpTry<Outcome>>()
        val bodyClosed = Job()

        val requestJob = requestScope.launch {
            try {
                client.prepareRequest(request.toKtorRequest()).execute { ktorResponse ->
                    val outcome = handleResponse(request, ktorResponse, bodyClosed)
                    val streaming = outcome.getOrNull() is Outcome.Response
                    responseReady.complete(outcome)
                    if (streaming) {
                        // Keep the network connection open until the consumer closes the body.
                        bodyClosed.join()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                responseReady.complete(Try.failure(mapHttpException(e)))
            }
        }

        // If the request job dies before producing a response — for example because the client
        // was closed — surface an error to the caller instead of a foreign cancellation.
        requestJob.invokeOnCompletion { cause ->
            if (cause != null) {
                responseReady.complete(
                    Try.failure(
                        HttpError.IO(
                            DebugError("The HTTP request was cancelled.", ThrowableError(cause))
                        )
                    )
                )
            }
        }

        return try {
            responseReady.await()
        } catch (e: CancellationException) {
            // Only the caller's own cancellation can get here, since [responseReady] is always
            // completed with a value.
            requestJob.cancel()
            throw e
        }
    }

    private suspend fun handleResponse(
        request: HttpRequest,
        ktorResponse: KtorHttpResponse,
        bodyClosed: CompletableJob,
    ): HttpTry<Outcome> {
        val statusCode = ktorResponse.status.value
        val mediaType = ktorResponse.headers["Content-Type"]?.let { MediaType(it) }

        if (statusCode >= 400) {
            // Reads the full body, since it might contain an error representation such as
            // JSON Problem Details or OPDS Authentication Document.
            val chunks = mutableListOf<ByteArray>()
            ktorResponse.bodyAsChannel().streamUpTo(Long.MAX_VALUE) { chunks.add(it) }
            val body = chunks.join()
            return Try.failure(
                HttpError.ErrorResponse(HttpStatus(statusCode), mediaType, body)
            )
        }

        val response = HttpResponse(
            request = request,
            url = request.url,
            statusCode = HttpStatus(statusCode),
            headers = ktorResponse.headers.toMap(),
            mediaType = mediaType
        )

        callback.onResponseReceived(request, response)

        if (statusCode in REDIRECT_STATUS_CODES) {
            return Try.success(Outcome.Redirect(response))
        }

        val body = HttpChannelReadable(
            channel = ktorResponse.bodyAsChannel(),
            knownLength = response.contentLength,
            onClose = { bodyClosed.complete() }
        )
        return Try.success(Outcome.Response(HttpStreamResponse(response, body)))
    }

    /**
     * Follows a redirection received as [response] to the given [request].
     *
     * Redirections to the same protocol are followed automatically (like `HttpURLConnection`
     * did), while cross-protocol redirections (e.g. HTTP to HTTPS) are considered unsafe and
     * require an explicit confirmation with [Callback.onFollowUnsafeRedirect].
     */
    private suspend fun followRedirect(
        request: HttpRequest,
        response: HttpResponse,
    ): HttpTry<HttpStreamResponse> {
        val redirectCount = request.extras[EXTRA_REDIRECT_COUNT]?.toIntOrNull() ?: 0
        if (redirectCount >= MAX_REDIRECTS) {
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

        val sameScheme = location.scheme == request.url.scheme
        val sameHost = location.host.equals(request.url.host, ignoreCase = true)

        // The original headers are preserved across redirections (like `HttpURLConnection`
        // did), except for the credential-bearing ones when the origin changes.
        var headers = request.headers.filterKeys { key ->
            (sameScheme && sameHost) || key.lowercase() !in CREDENTIAL_HEADERS
        }

        // Forwards the cookies set by the redirecting response, only to the same host.
        if (sameHost) {
            // The cookie pair is the part of a `Set-Cookie` value before the first attribute,
            // e.g. `session=1; Path=/; Secure` -> `session=1`.
            val cookies = response.headers("Set-Cookie")
                .mapNotNull { header ->
                    header.substringBefore(';').trim().takeIf { it.isNotEmpty() }
                }

            if (cookies.isNotEmpty()) {
                val existingCookies = headers.entries
                    .firstOrNull { it.key.lowercase() == "cookie" }?.value
                    ?: emptyList()

                headers = headers.filterKeys { it.lowercase() != "cookie" } +
                    ("Cookie" to listOf((existingCookies + cookies).joinToString("; ")))
            }
        }

        // Per RFC 9110, a 303 See Other redirection is followed with a GET request and no body.
        val changeToGet = response.statusCode.code == 303 &&
            request.method !in listOf(Method.GET, Method.HEAD)

        val newRequest = HttpRequest(
            url = location,
            method = if (changeToGet) Method.GET else request.method,
            headers = headers,
            body = if (changeToGet) null else request.body,
            extras = request.extras + (EXTRA_REDIRECT_COUNT to (redirectCount + 1).toString()),
            connectTimeout = request.connectTimeout,
            readTimeout = request.readTimeout,
            allowUserInteraction = request.allowUserInteraction
        )

        return if (sameScheme) {
            stream(newRequest)
        } else {
            callback
                .onFollowUnsafeRedirect(request, response = response, newRequest = newRequest)
                .flatMap { stream(it) }
        }
    }

    private fun HttpRequest.toKtorRequest(): HttpRequestBuilder {
        val request = this
        return HttpRequestBuilder().apply {
            url(request.url.toString())
            method = HttpMethod.parse(request.method.name)

            val normalizedHeaders = request.headers
                .lowerCaseKeys()
                .joinValues(",")

            if (userAgent != null && "user-agent" !in normalizedHeaders) {
                headers.append("User-Agent", userAgent)
            }

            for ((k, v) in normalizedHeaders) {
                headers.append(k, v)
            }

            timeout {
                (request.connectTimeout ?: this@DefaultHttpClient.connectTimeout)
                    ?.let { connectTimeoutMillis = it.toTimeoutMillis() }
                (request.readTimeout ?: this@DefaultHttpClient.readTimeout)
                    ?.let { socketTimeoutMillis = it.toTimeoutMillis() }
            }

            when (val body = request.body) {
                null -> {}
                is HttpRequest.Body.Bytes ->
                    setBody(body.bytes)
                is HttpRequest.Body.File ->
                    setBody(FileContent(body.url))
            }
        }
    }

    /**
     * A timeout of zero is interpreted as an infinite timeout, like `HttpURLConnection` did.
     */
    private fun Duration.toTimeoutMillis(): Long =
        inWholeMilliseconds.takeIf { it > 0 } ?: Long.MAX_VALUE
}

/**
 * Request body streaming the content of the file at the given `file://` [url].
 */
private class FileContent(
    private val url: AbsoluteUrl,
) : OutgoingContent.WriteChannelContent() {

    override suspend fun writeTo(channel: ByteWriteChannel) {
        val path = checkNotNull(url.path) { "Invalid file URL: $url" }.toPath()
        fileSystem.read(path) {
            val chunk = ByteArray(8192)
            while (true) {
                val read = read(chunk)
                if (read == -1) {
                    break
                }
                channel.writeFully(chunk, 0, read)
            }
        }
    }
}

/**
 * Masks the values of sensitive headers, to avoid leaking credentials in logs.
 */
private fun Map<String, List<String>>.redacted(): Map<String, List<String>> =
    mapValues { (key, value) ->
        when (key.lowercase()) {
            "authorization", "proxy-authorization", "cookie", "set-cookie" ->
                listOf("***")
            else -> value
        }
    }
