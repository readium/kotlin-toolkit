/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.util.Try

sealed class BaseBytesResource(
    val bytes: suspend () -> Try<ByteArray, Resource.Exception>
) : Resource {

    private lateinit var _bytes: Try<ByteArray, Resource.Exception>

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        if (!::_bytes.isInitialized)
            _bytes = bytes()

        if (range == null || _bytes.isFailure)
            return _bytes

        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceIn(0L until _bytes.getOrThrow().size)
            .requireLengthFitInt()

        return Try.success(_bytes.getOrThrow().sliceArray(range.map(Long::toInt)))
    }

    override suspend fun length(): ResourceTry<Long> =
        read().map { it.size.toLong() }

    override suspend fun close() {}
}

/** Creates a Resource serving a [ByteArray]. */
class BytesResource(bytes: suspend () -> Try<ByteArray, Resource.Exception>) : BaseBytesResource(bytes) {

    constructor(bytes: ByteArray) : this({ Try.success(bytes) })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() }} bytes)"
}

/** Creates a Resource serving a [String]. */
class StringResource(
    string: suspend () -> ResourceTry<String>
) : BaseBytesResource({ string().map { it.toByteArray() } }) {

    constructor(string: String) : this({ Try.success(string) })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { readAsString() }})"
}
