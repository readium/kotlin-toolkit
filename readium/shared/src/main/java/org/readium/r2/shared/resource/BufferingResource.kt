/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.error.Try
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.contains
import org.readium.r2.shared.extensions.requireLengthFitInt

/**
 * Wraps a [Resource] and buffers its content.
 *
 * Expensive interaction with the underlying resource is minimized, since most (smaller) requests
 * can be satisfied by accessing the buffer alone. The drawback is that some extra space is required
 * to hold the buffer and that copying takes place when filling that buffer, but this is usually
 * outweighed by the performance benefits.
 *
 * Note that this implementation is pretty limited and the benefits are only apparent when reading
 * forward and consecutively – e.g. when downloading the resource by chunks. The buffer is ignored
 * when reading backward or far ahead.
 *
 * @param resource Underlying resource which will be buffered.
 * @param resourceLength The total length of the resource, when known. This can improve performance
 *        by avoiding requesting the length from the underlying resource.
 * @param bufferSize Size of the buffer chunks to read.
 */
public class BufferingResource(
    private val resource: Resource,
    resourceLength: Long? = null,
    private val bufferSize: Long = DEFAULT_BUFFER_SIZE,
) : Resource by resource {

    internal companion object {
        internal const val DEFAULT_BUFFER_SIZE: Long = 8192
    }

    /**
     * The buffer containing the current bytes read from the wrapped [Resource], with the range it
     * covers.
     */
    private var buffer: Pair<ByteArray, LongRange>? = null

    private lateinit var _cachedLength: ResourceTry<Long>
    private suspend fun cachedLength(): ResourceTry<Long> {
        if (!::_cachedLength.isInitialized)
            _cachedLength = resource.length()
        return _cachedLength
    }

    init {
        if (resourceLength != null) {
            _cachedLength = Try.success(resourceLength)
        }
    }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        val length = cachedLength().getOrNull()
        // Reading the whole resource bypasses buffering to keep things simple.
        if (range == null || length == null) {
            return resource.read(range)
        }

        val requestedRange = range
            .coerceIn(0L until length)
            .requireLengthFitInt()
        if (requestedRange.isEmpty()) {
            return Try.success(ByteArray(0))
        }

        // Round up the range to be read to the next `bufferSize`, because we will buffer the
        // excess.
        val readLast = (requestedRange.last + 1).ceilMultipleOf(bufferSize).coerceAtMost(length)
        var readRange = requestedRange.first until readLast

        // Attempt to serve parts or all of the request using the buffer.
        buffer?.let { pair ->
            var (buffer, bufferedRange) = pair

            // Everything already buffered?
            if (bufferedRange.contains(requestedRange)) {
                val data = extractRange(requestedRange, buffer, start = bufferedRange.first)
                return Try.success(data)

                // Beginning of requested data is buffered?
            } else if (bufferedRange.contains(requestedRange.first)) {
                readRange = (bufferedRange.last + 1)..readRange.last

                return resource.read(readRange).map { readData ->
                    buffer += readData
                    // Shift the current buffer to the tail of the read data.
                    saveBuffer(buffer, readRange)

                    val bytes = extractRange(requestedRange, buffer, start = bufferedRange.first)
                    bytes
                }
            }
        }

        // Fallback on reading the requested range from the original resource.
        return resource.read(readRange).map { data ->
            saveBuffer(data, readRange)

            val res = if (data.count() > requestedRange.count())
                data.copyOfRange(0, requestedRange.count())
            else
                data

            res
        }
    }

    /**
     * Keeps the last chunk of the given data as the buffer for next reads.
     *
     * @param data Data read from the original resource.
     * @param range Range of the read data in the resource.
     */
    private fun saveBuffer(data: ByteArray, range: LongRange) {
        val lastChunk = data.takeLast(bufferSize.toInt()).toByteArray()
        val chunkRange = (range.last + 1 - lastChunk.count())..range.last
        buffer = Pair(lastChunk, chunkRange)
    }

    /**
     * Reads a sub-range of the given [data] after shifting the given absolute (to the resource)
     * ranges to be relative to [data].
     */
    private fun extractRange(requestedRange: LongRange, data: ByteArray, start: Long): ByteArray {
        val first = requestedRange.first - start
        val lastExclusive = first + requestedRange.count()
        require(first >= 0)
        require(lastExclusive <= data.count()) { "$lastExclusive > ${data.count()}" }
        return data.copyOfRange(first.toInt(), lastExclusive.toInt())
    }

    private fun Long.ceilMultipleOf(divisor: Long) =
        divisor * (this / divisor + if (this % divisor == 0L) 0 else 1)
}

/**
 * Wraps this resource in a [BufferingResource] to improve reading performances.
 *
 * @param resourceLength The total length of the resource, when known. This can improve performance
 *        by avoiding requesting the length from the underlying resource.
 * @param size Size of the buffer chunks to read.
 */
public fun Resource.buffered(
    resourceLength: Long? = null,
    size: Long = BufferingResource.DEFAULT_BUFFER_SIZE
): BufferingResource =
    BufferingResource(resource = this, resourceLength = resourceLength, bufferSize = size)
