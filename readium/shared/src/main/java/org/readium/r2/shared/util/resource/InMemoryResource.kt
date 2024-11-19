/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.resource

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError

/** Creates a [Resource] serving a [ByteArray]. */
public class InMemoryResource(
    override val sourceUrl: AbsoluteUrl?,
    private val properties: Resource.Properties,
    private val bytes: suspend () -> Try<ByteArray, ReadError>,
) : Resource {

    public constructor(
        bytes: ByteArray,
        source: AbsoluteUrl? = null,
        properties: Resource.Properties = Resource.Properties(),
    ) : this(sourceUrl = source, properties = properties, { Try.success(bytes) })

    private lateinit var _bytes: Try<ByteArray, ReadError>

    override suspend fun properties(): Try<Resource.Properties, ReadError> {
        return Try.success(properties)
    }

    override suspend fun length(): Try<Long, ReadError> =
        read().map { it.size.toLong() }

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

    override fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() }} bytes)"
}
