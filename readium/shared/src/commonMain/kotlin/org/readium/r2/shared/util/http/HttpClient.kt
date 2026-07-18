/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonObject
import okio.FileNotFoundException
import okio.IOException
import okio.Path.Companion.toPath
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.DEFAULT_BUFFER_SIZE
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.file.fileSystem
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.json.LenientJson
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
 * The [body] supports only forward reads: you can request any range starting at or after the
 * current position, but not going backward.
 *
 * You MUST close the [body] to terminate the HTTP connection when you're done.
 */
public class HttpStreamResponse(
    public val response: HttpResponse,
    public val body: Readable,
)

/**
 * Converts a [ReadError] occurring while reading a response body to an [HttpError].
 */
internal fun ReadError.toHttpError(): HttpError =
    (this as? ReadError.Access)?.cause as? HttpError
        ?: HttpError.IO(this)

/**
 * Fetches the resource from the given [request].
 */
public suspend fun HttpClient.fetch(request: HttpRequest): HttpTry<HttpFetchResponse> =
    stream(request)
        .flatMap { response ->
            try {
                response.body.read()
                    .mapFailure { it.toHttpError() }
                    .map { HttpFetchResponse(response.response, it) }
            } finally {
                response.body.close()
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
 * Fetches the resource from the given [request] as a UTF-8 [String].
 */
public suspend fun HttpClient.fetchString(request: HttpRequest): HttpTry<String> =
    fetchWithDecoder(request) { response ->
        response.body.decodeToString()
    }

/**
 * Fetches the resource from the given [request] as a [JsonObject].
 */
public suspend fun HttpClient.fetchJSONObject(request: HttpRequest): HttpTry<JsonObject> =
    fetchWithDecoder(request) { response ->
        LenientJson.parseToJsonElement(response.body.decodeToString()) as JsonObject
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
 * Downloads the resource from the given [request] to the file at the [destination] URL.
 *
 * @param request The [HttpRequest] detailing the resource to be downloaded.
 * @param destination `file://` URL where the downloaded resource should be saved.
 * @param onProgress A closure called regularly with the download progress, from 0.0 to 1.0.
 */
public suspend fun HttpClient.download(
    request: HttpRequest,
    destination: AbsoluteUrl,
    onProgress: (Double) -> Unit = {},
): Try<HttpResponse, HttpDownloadError> =
    stream(request)
        .mapFailure {
            HttpDownloadError.Http(cause = it)
        }
        .flatMap { response ->
            val expectedLength = response.response.contentLength
                ?.toDouble()
                ?.takeIf { it > 0 }

            try {
                response.body.copyToFile(
                    destination = destination,
                    onProgress = { readLength ->
                        if (expectedLength != null) {
                            val progress = (readLength / expectedLength).coerceIn(0.0, 1.0)
                            onProgress(progress)
                        }
                    }
                ).map {
                    response.response
                }
            } finally {
                response.body.close()
            }
        }

private suspend fun Readable.copyToFile(
    destination: AbsoluteUrl,
    onProgress: (Long) -> Unit,
): Try<Unit, HttpDownloadError> {
    val path = destination.path?.toPath()
        ?: return Try.failure(
            HttpDownloadError.Filesystem(
                FileSystemError.IO(
                    ThrowableError(IllegalArgumentException("Invalid destination file URL: $destination"))
                )
            )
        )

    var position = 0L

    try {
        fileSystem.write(path) {
            while (true) {
                currentCoroutineContext().ensureActive()

                val chunk = read(position until position + DEFAULT_BUFFER_SIZE)
                    .getOrElse { error ->
                        tryOrLog { fileSystem.delete(path) }
                        return Try.failure(
                            HttpDownloadError.Http(error.toHttpError())
                        )
                    }

                if (chunk.isEmpty()) {
                    break
                }

                position += chunk.size
                write(chunk)
                onProgress(position)
            }
        }
    } catch (e: FileNotFoundException) {
        return Try.failure(
            HttpDownloadError.Filesystem(FileSystemError.FileNotFound(e))
        )
    } catch (e: IOException) {
        tryOrLog { fileSystem.delete(path) }
        return Try.failure(
            HttpDownloadError.Filesystem(FileSystemError.IO(e))
        )
    }

    return Try.success(Unit)
}
