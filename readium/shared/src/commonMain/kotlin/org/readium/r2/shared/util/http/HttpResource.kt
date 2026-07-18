/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType

/**
 * Provides access to an external URL through HTTP.
 *
 * Known limitations, kept for parity with the pre-KMP implementation:
 * - a 206 Partial Content response is trusted without validating the `Content-Range` offset;
 * - concurrent calls to [read] are not synchronized — callers must serialize their reads.
 */
@OptIn(ExperimentalReadiumApi::class)
public class HttpResource(
    override val sourceUrl: AbsoluteUrl,
    private val client: HttpClient,
    private val maxSkipBytes: Long = MAX_SKIP_BYTES,
) : Resource {

    /**
     * Cached HTTP response body, streamed by forward reads.
     *
     * @param start Absolute offset in the remote resource matching the beginning of [body].
     */
    private class Session(val body: Readable, val start: Long) {
        /** Absolute offset in the remote resource of the next byte to read from [body]. */
        var position: Long = start
    }

    private var session: Session? = null

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

    override fun close() {
        tryOrLog {
            session?.body?.close()
        }
        session = null
    }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
        val from = range?.first?.takeUnless { it == 0L }

        return acquireSession(from).flatMap { session ->
            val relativeRange = range?.let {
                val start = session.position - session.start
                start until start + (it.last - it.first + 1)
            }

            session.body.read(relativeRange)
                .onSuccess { session.position += it.size }
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
     * Returns a session positioned at the [from] byte offset.
     *
     * The HTTP response body is cached and reused for next calls, if the next [from] offset is
     * not too far and in a forward direction.
     */
    private suspend fun acquireSession(from: Long?): Try<Session, ReadError> {
        val session = this.session
        if (from != null && session != null) {
            val bytesToSkip = from - session.position
            if (bytesToSkip in 0 until maxSkipBytes) {
                if (bytesToSkip > 0L) {
                    tryOrLog {
                        session.body
                            .read((session.position - session.start) until (from - session.start))
                            .onSuccess { session.position += it.size }
                    }
                }
                if (session.position == from) {
                    return Try.success(session)
                }
            }
        }

        tryOrLog { session?.body?.close() }
        this.session = null

        val request = HttpRequest(sourceUrl) {
            from?.let { setRange(from..-1) }
        }

        return client.stream(request)
            .mapFailure { ReadError.Access(it) }
            .flatMap { response ->
                if (from != null && response.response.statusCode.code != 206) {
                    response.body.close()
                    val error = DebugError(
                        "Server seems not to support range requests to $sourceUrl."
                    )
                    Try.failure(ReadError.UnsupportedOperation(error))
                } else {
                    Try.success(Session(response.body, from ?: 0))
                }
            }
            .onSuccess { this.session = it }
    }

    public companion object {

        private const val MAX_SKIP_BYTES: Long = 8192
    }
}
