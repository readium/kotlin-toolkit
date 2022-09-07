/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.InputStream
import java.nio.charset.Charset

/**
 * An HTTP client performs HTTP requests.
 *
 * You may provide a custom implementation, or use the [DefaultHttpClient] one which relies on
 * native APIs.
 */
interface HttpClient {

    /**
     * Streams the resource from the given [request].
     */
    suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse>

    /**
     * Fetches the resource from the given [request].
     */
    suspend fun fetch(request: HttpRequest): HttpTry<HttpFetchResponse> =
        stream(request)
            .flatMap { response ->
                try {
                    val body = withContext(Dispatchers.IO) {
                        response.body.use { it.readBytes() }
                    }
                    Try.success(HttpFetchResponse(response.response, body))
                } catch (e: Exception) {
                    Try.failure(HttpException.wrap(e))
                }
            }

    // Declare a companion object to allow reading apps to extend it. For example, by adding a
    // HttpClient.get(Context) constructor.
    companion object

}

/**
 * Fetches the resource from the given [request] before decoding it with the provided [decoder].
 *
 * If the decoder fails, a MalformedResponse error is returned.
 */
suspend fun <T> HttpClient.fetchWithDecoder(request: HttpRequest, decoder: (HttpFetchResponse) -> T): HttpTry<T> =
    fetch(request)
        .flatMap {
            try {
                Try.success(decoder(it))
            } catch (e: Exception) {
                Try.failure(HttpException(kind = HttpException.Kind.MalformedResponse, cause = e))
            }
        }

/**
 * Fetches the resource from the given [request] as a [String].
 */
suspend fun HttpClient.fetchString(request: HttpRequest, charset: Charset = Charsets.UTF_8): HttpTry<String> =
    fetchWithDecoder(request) { response ->
        String(response.body, charset)
    }

/**
 * Fetches the resource from the given [request] as a [JSONObject].
 */
suspend fun HttpClient.fetchJSONObject(request: HttpRequest): HttpTry<JSONObject> =
    fetchWithDecoder(request) { response ->
        JSONObject(String(response.body))
    }

class HttpStreamResponse(
    val response: HttpResponse,
    val body: InputStream,
)

class HttpFetchResponse(
    val response: HttpResponse,
    val body: ByteArray,
)

/**
 * Represents a successful HTTP response received from a server.
 *
 * @param request Request associated with the response.
 * @param url Final URL of the response.
 * @param statusCode Response status code.
 * @param headers HTTP response headers, indexed by their name.
 * @param mediaType Media type sniffed from the `Content-Type` header and response body. Falls back
 *        on `application/octet-stream`.
 */
data class HttpResponse(
    val request: HttpRequest,
    val url: String,
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val mediaType: MediaType,
) {

    private val httpHeaders = HttpHeaders(headers)

    /**
     * Finds the first value of the first header matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     */
    fun valueForHeader(name: String): String? = httpHeaders[name]

    /**
     * Finds all the values of the first header matching the given name.
     * In keeping with the HTTP RFC, HTTP header field names are case-insensitive.
     */
    fun valuesForHeader(name: String): List<String> = httpHeaders.getAll(name)

    /**
     * Indicates whether this server supports byte range requests.
     */
    val acceptsByteRanges: Boolean get() = httpHeaders.acceptsByteRanges

    /**
     * The expected content length for this response, when known.
     *
     * Warning: For byte range requests, this will be the length of the chunk, not the full
     * resource.
     */
    val contentLength: Long? get() = httpHeaders.contentLength
}