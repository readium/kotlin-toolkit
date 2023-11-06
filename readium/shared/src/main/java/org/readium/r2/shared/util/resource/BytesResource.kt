/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.assertSuccess
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType

public sealed class BaseBytesResource(
    override val source: AbsoluteUrl?,
    private val mediaType: MediaType,
    private val properties: Resource.Properties,
    protected val bytes: suspend () -> Try<ByteArray, ReadError>
) : Resource {

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        Try.success(properties)

    override suspend fun mediaType(): Try<MediaType, ReadError> =
        Try.success(mediaType)

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
}

/** Creates a Resource serving a [ByteArray]. */
public class BytesResource(
    source: AbsoluteUrl? = null,
    mediaType: MediaType,
    properties: Resource.Properties = Resource.Properties(),
    bytes: suspend () -> Try<ByteArray, ReadError>
) : BaseBytesResource(
    source = source,
    mediaType = mediaType,
    properties = properties,
    bytes = bytes
) {

    public constructor(
        bytes: ByteArray,
        mediaType: MediaType,
        url: AbsoluteUrl? = null,
        properties: Resource.Properties = Resource.Properties()
    ) :
        this(source = url, mediaType = mediaType, properties = properties, { Try.success(bytes) })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() }} bytes)"
}

/** Creates a Resource serving a [String]. */
public class StringResource(
    source: AbsoluteUrl? = null,
    mediaType: MediaType,
    properties: Resource.Properties = Resource.Properties(),
    string: suspend () -> Try<String, ReadError>
) : BaseBytesResource(
    source = source,
    mediaType = mediaType,
    properties = properties,
    { string().map { it.toByteArray() } }
) {

    public constructor(
        string: String,
        mediaType: MediaType,
        url: AbsoluteUrl? = null,
        properties: Resource.Properties = Resource.Properties()
    ) :
        this(source = url, mediaType = mediaType, properties = properties, { Try.success(string) })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { read().assertSuccess().decodeToString() } }})"
}
