/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType

/** Provides access to an external URL through HTTP. */
@OptIn(ExperimentalReadiumApi::class)
public class HttpResource(
    override val sourceUrl: AbsoluteUrl,
    private val client: HttpClient,
    private val maxSkipBytes: Long = MAX_SKIP_BYTES,
) : Resource {

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        headResponse().map {
            Resource.Properties(
                Resource.Properties.Builder()
                    .apply {
                        mediaType = it.mediaType
                        filename = it.url.filename
                    }
            )
        }

    override suspend fun length(): Try<Long, ReadError> =
        headResponse().flatMap {
            val contentLength = it.contentLength
            return if (contentLength != null) {
                Try.success(contentLength)
            } else {
                Try.failure(
                    ReadError.UnsupportedOperation(
                        DebugError(
                            "Server did not provide content length in its response to request to $sourceUrl."
                        )
                    )
                )
            }
        }

    override fun close() {}

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> = withContext(
        Dispatchers.IO
    ) {
        try {
            stream(range?.first.takeUnless { it == 0L }).map { stream ->
                if (range != null) {
                    stream.read(range.count().toLong())
                } else {
                    stream.readBytes()
                }
            }
        } catch (e: IOException) {
            Try.failure(ReadError.Access(HttpError.IO(e)))
        }
    }

    /** Cached HEAD response to get the expected content length and other metadata. */
    private lateinit var _headResponse: Try<HttpResponse, ReadError>

    private suspend fun headResponse(): Try<HttpResponse, ReadError> {
        if (::_headResponse.isInitialized) {
            return _headResponse
        }

        _headResponse = client.head(HttpRequest(sourceUrl))
            .mapFailure { ReadError.Access(it) }

        return _headResponse
    }

    /**
     * Returns an HTTP stream for the resource, starting at the [from] byte offset.
     *
     * The stream is cached and reused for next calls, if the next [from] offset is not too far
     * and in a forward direction.
     */
    private suspend fun stream(from: Long? = null): Try<InputStream, ReadError> {
        val stream = inputStream
        if (from != null && stream != null) {
            tryOrLog {
                val bytesToSkip = from - (inputStreamStart + stream.count)
                if (bytesToSkip in 0 until maxSkipBytes) {
                    stream.skip(bytesToSkip)
                    return Try.success(stream)
                }
            }
        }
        tryOrLog { inputStream?.close() }

        val request = HttpRequest(sourceUrl) {
            from?.let { setRange(from..-1) }
        }

        return client.stream(request)
            .mapFailure { ReadError.Access(it) }
            .flatMap { response ->
                if (from != null && response.response.statusCode.code != 206) {
                    val error = DebugError(
                        "Server seems not to support range requests to $sourceUrl."
                    )
                    Try.failure(ReadError.UnsupportedOperation(error))
                } else {
                    Try.success(response)
                }
            }
            .map { CountingInputStream(it.body) }
            .onSuccess {
                inputStream = it
                inputStreamStart = from ?: 0
            }
    }

    private var inputStream: CountingInputStream? = null
    private var inputStreamStart = 0L

    public companion object {

        private const val MAX_SKIP_BYTES: Long = 8192
    }
}
