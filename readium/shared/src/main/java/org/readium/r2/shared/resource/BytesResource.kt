/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType

public sealed class BaseBytesResource(
    override val source: AbsoluteUrl?,
    private val mediaType: MediaType,
    private val properties: Resource.Properties,
    protected val bytes: suspend () -> Try<ByteArray, Resource.Exception>
) : Resource {

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        Try.success(properties)

    override suspend fun mediaType(): ResourceTry<MediaType> =
        Try.success(mediaType)

    override suspend fun length(): ResourceTry<Long> =
        read().map { it.size.toLong() }

    private lateinit var _bytes: Try<ByteArray, Resource.Exception>

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        if (!::_bytes.isInitialized) {
            _bytes = bytes()
        }

        if (range == null) {
            return _bytes
        }

        return _bytes.map { it.read(range) }
    }

    override suspend fun close() {}
}

/** Creates a Resource serving a [ByteArray]. */
public class BytesResource(
    url: AbsoluteUrl? = null,
    mediaType: MediaType,
    properties: Resource.Properties = Resource.Properties(),
    bytes: suspend () -> ResourceTry<ByteArray>
) : BaseBytesResource(source = url, mediaType = mediaType, properties = properties, bytes = bytes) {

    public constructor(
        bytes: ByteArray,
        mediaType: MediaType,
        url: AbsoluteUrl? = null,
        properties: Resource.Properties = Resource.Properties()
    ) :
        this(url = url, mediaType = mediaType, properties = properties, { Try.success(bytes) })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() }} bytes)"
}

/** Creates a Resource serving a [String]. */
public class StringResource(
    url: AbsoluteUrl? = null,
    mediaType: MediaType,
    properties: Resource.Properties = Resource.Properties(),
    string: suspend () -> ResourceTry<String>
) : BaseBytesResource(
    source = url,
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
        this(url = url, mediaType = mediaType, properties = properties, { Try.success(string) })

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { readAsString() }})"
}
