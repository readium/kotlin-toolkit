/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.requireLengthFitInt

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
    private val cacheBytes: Boolean = true
) : Resource by resource {

    public companion object {
        /**
         * Creates a [TransformingResource] using the given [transform] function.
         */
        public operator fun invoke(
            resource: Resource,
            transform: suspend (ByteArray) -> ResourceTry<ByteArray>
        ): TransformingResource =
            object : TransformingResource(resource) {
                override suspend fun transform(data: ResourceTry<ByteArray>): ResourceTry<ByteArray> =
                    data.flatMap { transform(it) }
            }
    }

    private lateinit var _bytes: ResourceTry<ByteArray>

    public abstract suspend fun transform(data: ResourceTry<ByteArray>): ResourceTry<ByteArray>

    private suspend fun bytes(): ResourceTry<ByteArray> {
        if (::_bytes.isInitialized)
            return _bytes

        val bytes = transform(resource.read())
        if (cacheBytes)
            _bytes = bytes

        return bytes
    }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        bytes().map {
            if (range == null)
                return bytes()

            @Suppress("NAME_SHADOWING")
            val range = range
                .coerceIn(0L until it.size)
                .requireLengthFitInt()

            it.sliceArray(range.map(Long::toInt))
        }

    override suspend fun length(): ResourceTry<Long> = bytes().map { it.size.toLong() }
}
