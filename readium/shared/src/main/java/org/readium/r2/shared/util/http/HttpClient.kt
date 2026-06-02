/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.math.round
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.tryRecover

public typealias HttpTry<SuccessT> = Try<SuccessT, HttpError>

/**
 * An HTTP client performs HTTP requests.
 *
 * You may provide a custom implementation, or use the [DefaultHttpClient] one which relies on
 * native APIs.
 */
public interface HttpClient {

    /**
     * Streams the resource from the given [request].
     */
    public suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse>

    // Declare a companion object to allow reading apps to extend it. For example, by adding a
    // HttpClient.get(Context) constructor.
    public companion object
}

/**
 * HTTP response with streamable content.
 *
 * You MUST close the [body] to terminate the HTTP connection when you're done.
 */
public class HttpStreamResponse(
    public val response: HttpResponse,
    public val body: InputStream,
)

/**
 * Fetches the resource from the given [request].
 */
public suspend fun HttpClient.fetch(request: HttpRequest): HttpTry<HttpFetchResponse> =
    stream(request)
        .flatMap { response ->
            try {
                val body = withContext(Dispatchers.IO) {
                    response.body.use { it.readBytes() }
                }
                Try.success(
                    HttpFetchResponse(response.response, body)
                )
            } catch (e: IOException) {
                Try.failure(
                    HttpError.IO(e)
                )
            }
        }

/**
 * Fetches the resource from the given [request] before decoding it with the provided [decoder].
 *
 * If the decoder fails, a MalformedResponse error is returned.
 */
public suspend fun <T> HttpClient.fetchWithDecoder(
    request: HttpRequest,
    decoder: (HttpFetchResponse) -> T,
): HttpTry<T> =
    fetch(request)
        .flatMap {
            try {
                Try.success(
                    decoder(it)
                )
            } catch (e: Exception) {
                Try.failure(
                    HttpError.MalformedResponse(ThrowableError(e))
                )
            }
        }

/**
 * Fetches the resource from the given [request] as a [String].
 */
public suspend fun HttpClient.fetchString(request: HttpRequest, charset: Charset = Charsets.UTF_8): HttpTry<String> =
    fetchWithDecoder(request) { response ->
        String(response.body, charset)
    }

/**
 * Fetches the resource from the given [request] as a [JSONObject].
 */
public suspend fun HttpClient.fetchJSONObject(request: HttpRequest): HttpTry<JSONObject> =
    fetchWithDecoder(request) { response ->
        JSONObject(String(response.body))
    }

/**
 * HTTP response with the whole [body] as a [ByteArray].
 */
public class HttpFetchResponse(
    public val response: HttpResponse,
    public val body: ByteArray,
)

/**
 * Performs a HEAD request to retrieve only the response headers.
 *
 * This helper falls back on a GET request with 0-length byte range if the server doesn't support
 * HEAD requests.
 */
@ExperimentalReadiumApi
public suspend fun HttpClient.head(request: HttpRequest): HttpTry<HttpResponse> {
    suspend fun HttpRequest.response(): HttpTry<HttpResponse> =
        stream(this)
            .map { response ->
                response.body.close()
                response.response
            }

    return request
        .copy { method = HttpRequest.Method.HEAD }
        .response()
        .tryRecover { error ->
            if (error !is HttpError.ErrorResponse || error.status != HttpStatus.MethodNotAllowed) {
                return@tryRecover Try.failure(error)
            }

            request
                .copy {
                    method = HttpRequest.Method.GET
                    setRange(0L..0L)
                }
                .response()
        }
}

/**
 * Downloads the resource from the given [request] to the [destination] file.
 *
 * @param request The [HttpRequest] detailing the resource to be downloaded.
 * @param destination The [File] where the downloaded resource should be saved.
 * @param onProgress A closure called regularly with the download progress, from 0.0 to 1.0.
 */
public suspend fun HttpClient.download(
    request: HttpRequest,
    destination: File,
    onProgress: (Double) -> Unit = {},
): Try<HttpResponse, HttpDownloadError> =
    stream(request).mapFailure { HttpDownloadError.Http(error = it) }
        .flatMap { response ->
            try {
                withContext(Dispatchers.IO) {
                    coroutineContext.ensureActive()

                    var readLength = 0L
                    val expectedLength = response.response.contentLength?.toDouble()

                    var lastProgress = 0.0

                    try {
                        FileOutputStream(destination).use { out ->
                            val inputResult = try {
                                response.body.use { input ->
                                    val buf = ByteArray(size = 2048)
                                    while (true) {
                                        coroutineContext.ensureActive()
                                        val n = try {
                                            input.read(buf)
                                        } catch (e: Exception) {
                                            if (e is CancellationException) throw e
                                            return@withContext Try.failure(
                                                HttpDownloadError.Http(
                                                    error = HttpError.IO(
                                                        exception = e
                                                    )
                                                )
                                            )
                                        }

                                        if (n == -1) break

                                        coroutineContext.ensureActive()
                                        out.write(buf, 0, n)

                                        readLength += n

                                        if (expectedLength != null && expectedLength > 0) {
                                            val progress =
                                                (readLength / expectedLength).coerceIn(0.0, 1.0)
                                                    .roundToDecimals(decimals = 2)
                                            if (lastProgress < progress) {
                                                withContext(Dispatchers.Main) {
                                                    onProgress(progress)
                                                }
                                            }
                                            lastProgress = progress
                                        }
                                    }
                                }
                                Try.success(success = Unit)
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Try.failure(
                                    failure = HttpDownloadError.Http(
                                        error = HttpError.IO(
                                            exception = e
                                        )
                                    )
                                )
                            }

                            if (inputResult.isFailure) {
                                return@withContext inputResult.map { response.response }
                            }
                        }
                        Try.success(success = response.response)
                    } catch (e: SecurityException) {
                        Try.failure(
                            failure = HttpDownloadError.Filesystem(
                                error = FileSystemError.Forbidden(
                                    exception = e
                                )
                            )
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Try.failure(
                            failure = HttpDownloadError.Filesystem(
                                error = FileSystemError.IO(
                                    exception = e
                                )
                            )
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            }
        }

private fun Double.roundToDecimals(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}
