/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try

/** Creates a Blob serving a [ByteArray]. */
public class InMemoryBlob(
    override val source: AbsoluteUrl?,
    private val bytes: suspend () -> Try<ByteArray, ReadError>
) : Blob {

    public constructor(
        bytes: ByteArray,
        source: AbsoluteUrl? = null
    ) : this(source = source, { Try.success(bytes) })

    override suspend fun length(): Try<Long, ReadError> =
        read().map { it.size.toLong() }

    private lateinit var _bytes: Try<ByteArray, ReadError>

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
        if (!::_bytes.isInitialized) {
            _bytes = bytes()
        }

        if (range == null) {
            return _bytes
        }

        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceFirstNonNegative()
            .requireLengthFitInt()

        if (range.isEmpty()) {
            return Try.success(ByteArray(0))
        }

        return _bytes.map { it.read(range) }
    }

    override suspend fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() }} bytes)"
}
