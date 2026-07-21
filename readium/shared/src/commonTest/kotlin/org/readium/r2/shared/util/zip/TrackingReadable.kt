/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable

/**
 * An in-memory [Readable] fake recording the ranges of every read, to assert the read patterns of
 * the channel adapters (e.g. that zip-over-HTTP performs random access instead of full reads).
 */
class TrackingReadable(
    private val data: ByteArray,
) : Readable {

    val reads: MutableList<LongRange> = mutableListOf()

    var isClosed: Boolean = false
        private set

    override suspend fun length(): Try<Long, ReadError> =
        Try.success(data.size.toLong())

    // Implements `stream()` rather than `read()` so that both paths are recorded: `read()` is
    // derived from `stream()` by the `Readable` default.
    override suspend fun stream(
        range: LongRange?,
        consume: (ByteArray) -> Unit,
    ): Try<Unit, ReadError> {
        val start = (range?.first ?: 0L).coerceIn(0L, data.size.toLong())
        val endExclusive = (range?.let { it.last + 1 } ?: data.size.toLong())
            .coerceIn(start, data.size.toLong())
        reads.add(start until endExclusive)
        consume(data.copyOfRange(start.toInt(), endExclusive.toInt()))
        return Try.success(Unit)
    }

    override fun close() {
        isClosed = true
    }
}

/** Test data: `size` bytes with predictable values. */
fun testData(size: Int): ByteArray =
    ByteArray(size) { (it % 251).toByte() }
