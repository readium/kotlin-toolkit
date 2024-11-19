/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.flatMap

/**
 * Transforms the bytes of [resource] on-the-fly.
 *
 * If you set [cacheBytes] to false, consider providing your own implementation of [length] to avoid
 * unnecessary transformations.
 *
 * Warning: The transformation runs on the full content of [resource], so it's not appropriate for
 * large resources which can't be held in memory.
 */
public abstract class TransformingResource(
    private val resource: Resource,
    private val cacheBytes: Boolean = true,
) : Resource by resource {

    public companion object {
        /**
         * Creates a [TransformingResource] using the given [transform] function.
         */
        public operator fun invoke(
            resource: Resource,
            transform: suspend (ByteArray) -> Try<ByteArray, ReadError>,
        ): TransformingResource =
            object : TransformingResource(resource) {
                override suspend fun transform(data: Try<ByteArray, ReadError>): Try<ByteArray, ReadError> =
                    data.flatMap {
                        try {
                            transform(it)
                        } catch (e: OutOfMemoryError) {
                            Try.failure(ReadError.OutOfMemory(e))
                        }
                    }
            }
    }

    override val sourceUrl: AbsoluteUrl? = null

    private lateinit var _bytes: Try<ByteArray, ReadError>

    public abstract suspend fun transform(data: Try<ByteArray, ReadError>): Try<ByteArray, ReadError>

    private suspend fun bytes(): Try<ByteArray, ReadError> {
        if (::_bytes.isInitialized) {
            return _bytes
        }

        val bytes = transform(resource.read())
        if (cacheBytes) {
            _bytes = bytes
        }

        return bytes
    }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> =
        bytes().map {
            if (range == null) {
                return bytes()
            }

            @Suppress("NAME_SHADOWING")
            val range = range
                .coerceIn(0L until it.size)
                .requireLengthFitInt()

            it.sliceArray(range.map(Long::toInt))
        }

    override suspend fun length(): Try<Long, ReadError> =
        bytes().map { it.size.toLong() }
}

public fun Resource.map(transform: suspend (ByteArray) -> Try<ByteArray, ReadError>): Resource =
    TransformingResource(this, transform = transform)
