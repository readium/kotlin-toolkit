/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.coerceToPositiveIncreasing
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.util.Try
import java.io.InputStream

internal abstract class StreamResource : Resource {

    abstract fun stream(): ResourceTry<InputStream>

    /** An estimate of data length from metadata */
    protected abstract val metadataLength: Long?

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        if (range == null)
            readFully()
        else
            readRange(range)

    private suspend fun readFully(): ResourceTry<ByteArray> =
        stream().mapCatching { stream ->
            withContext(Dispatchers.IO) {
                stream.use {
                    it.readBytes()
                }
            }
        }

    private suspend fun readRange(range: LongRange): ResourceTry<ByteArray> {
        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceToPositiveIncreasing()
            .requireLengthFitInt()

        return stream().mapCatching { stream ->
            withContext(Dispatchers.IO) {
                stream.use {
                    val skipped = it.skip(range.first)
                    val length = range.last - range.first + 1
                    val bytes = it.read(length)
                    if (skipped != range.first && bytes.isNotEmpty()) {
                        throw Exception("Unable to skip enough bytes")
                    }
                    return@use bytes
                }
            }
        }
    }

    override suspend fun length(): ResourceTry<Long> =
        metadataLength?.let { Try.success(it) }
            ?: readFully().map { it.size.toLong() }
}
