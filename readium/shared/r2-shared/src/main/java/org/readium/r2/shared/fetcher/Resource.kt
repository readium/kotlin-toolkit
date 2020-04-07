/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import java.io.InputStream
import java.lang.IllegalArgumentException

interface Resource {
    val link: Link

    fun read(range: LongRange? = null): Try<ByteArray, Error>

    /** An estimate of data length. */
    val length: Try<Long, Error>

    fun close()

    sealed class Error {

        /** Equivalent to a 404 HTTP error. */
        object NotFound : Error()

        /** Equivalent to a 403 HTTP error. */
        object Forbidden : Error()

        /** Equivalent to a 503 HTTP error. */
        object Unavailable : Error()

        /** For any other error, such as HTTP 500. */
        class Other(val exception: Exception) : Error()
    }
}

typealias ResourceTransformer = (Resource) -> Resource

class FailureResource(override val link: Link, private val error: Resource.Error) : Resource {

    override fun read(range: LongRange?): Try<ByteArray, Resource.Error> = Try.failure(error)

    override val length:  Try<Long, Resource.Error> = Try.failure(error)

    override fun close() {}
}

internal abstract class StreamResource : Resource {

    protected abstract fun stream(): Try<InputStream, Resource.Error>

    /** An estimate of data length from metadata */
    protected abstract val metadataLength: Long?

    override fun read(range: LongRange?): Try<ByteArray, Resource.Error> =
        if (range == null)
            readFully()
        else
            readRange(range)

    private fun readFully(): Try<ByteArray, Resource.Error> {
        val streamResult = stream()
        if (streamResult.isFailure) return Try.failure(streamResult.failure)
        val stream = streamResult.success

        return stream.use {
            try {
                Try.success(it.readBytes())
            } catch (e: Exception) {
                Try.failure(Resource.Error.Other(e))
            }
        }
    }

    private fun readRange(range: LongRange): Try<ByteArray, Resource.Error> {
        checkRange(range)

        val streamResult = stream()
        if (streamResult.isFailure) return Try.failure(streamResult.failure)
        val stream = streamResult.success

        stream.use {
                val skipped = it.skip(range.first)
                if (skipped != range.first) return Try.failure(Resource.Error.Other(Exception("Unable to skip enough bytes")))
                val length = range.last - range.first + 1
                return try {
                    Try.success(it.read(length))
                } catch (e: Exception) {
                    Try.failure(Resource.Error.Other(e))
            }
        }
    }

    override val length: Try<Long, Resource.Error>
        get() {
            metadataLength?.let { return Try.success(it) }
            return readFully().map { it.size.toLong() }
        }
}

class BytesResource(override val link: Link, private val bytes: ByteArray) : Resource {

    override fun read(range: LongRange?): Try<ByteArray, Resource.Error> {
        if (range == null)
            return Try.success(bytes.copyOf())

        try {
            checkRange(range)
        } catch (e: Exception) {
            return Try.failure(Resource.Error.Other(Exception("Range not satisfiable")))
        }

        val byteRange = bytes.sliceArray(range.map(Long::toInt))
        return Try.success(byteRange)
    }

    override val length: Try<Long, Resource.Error> = Try.success(bytes.size.toLong())

    override fun close() {}
}

private fun checkRange(range: LongRange) {
    if (range.first > range.last || range.first < 0)
        throw IllegalArgumentException("Invalid range $range")
    else if (range.last - range.first + 1 > Int.MAX_VALUE)
        throw IllegalArgumentException("Range length greater than Int.MAX_VALUE")
}