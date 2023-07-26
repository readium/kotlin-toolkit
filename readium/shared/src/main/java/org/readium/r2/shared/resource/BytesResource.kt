/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import java.io.File
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.requireLengthFitInt

public sealed class BaseBytesResource(
    override val key: String? = null,
    private val mediaType: String? = null,
    protected val bytes: suspend () -> Try<ByteArray, Resource.Exception>
) : Resource {

    override val file: File? = null

    override suspend fun name(): ResourceTry<String?> =
        Try.success(null)

    override suspend fun mediaType(): ResourceTry<String?> =
        Try.success(mediaType)

    private lateinit var _bytes: Try<ByteArray, Resource.Exception>

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        if (!::_bytes.isInitialized)
            _bytes = bytes()

        if (range == null)
            return _bytes

        return _bytes.map { it.read(range) }
    }

    private fun ByteArray.read(range: LongRange): ByteArray {
        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceIn(0L until size)
            .requireLengthFitInt()

        return sliceArray(range.map(Long::toInt))
    }

    override suspend fun length(): ResourceTry<Long> =
        read().map { it.size.toLong() }

    override suspend fun close() {}
}

/** Creates a Resource serving a [ByteArray]. */
public class BytesResource(
    key: String? = null,
    mediaType: String? = null,
    bytes: suspend () -> Try<ByteArray, Resource.Exception>
) : BaseBytesResource(key = key, mediaType = mediaType, bytes) {

    public constructor(bytes: ByteArray, key: String? = null, mediaType: String? = null) :
        this(key = key, mediaType = mediaType, { Try.success(bytes) })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() }} bytes)"
}

/** Creates a Resource serving a [String]. */
public class StringResource(
    key: String? = null,
    val mediaType: String? = null,
    string: suspend () -> ResourceTry<String>
) : BaseBytesResource(key = key, mediaType = mediaType, { string().map { it.toByteArray() } }) {

    public constructor(string: String, mediaType: String? = null, key: String? = null) :
        this(key = key, mediaType = mediaType, { Try.success(string) })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { readAsString() }})"
}
