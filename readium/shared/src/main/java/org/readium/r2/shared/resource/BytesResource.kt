/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

public sealed class BaseBytesResource(
    override val url: Url?,
    private val mediaType: MediaType?,
    private val properties: Resource.Properties,
    protected val bytes: suspend () -> Try<ByteArray, Resource.Exception>
) : Resource {

    override suspend fun name(): ResourceTry<String?> =
        Try.success(null)

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        Try.success(properties)

    override suspend fun mediaType(): ResourceTry<MediaType?> =
        Try.success(mediaType)

    override suspend fun length(): ResourceTry<Long> =
        read().map { it.size.toLong() }

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

    override suspend fun close() {}
}

/** Creates a Resource serving a [ByteArray]. */
public class BytesResource(
    url: Url? = null,
    mediaType: MediaType? = null,
    properties: Resource.Properties = Resource.Properties(),
    bytes: suspend () -> ResourceTry<ByteArray>
) : BaseBytesResource(url = url, mediaType = mediaType, properties = properties, bytes = bytes) {

    public constructor(
        bytes: ByteArray,
        url: Url? = null,
        mediaType: MediaType? = null,
        properties: Resource.Properties = Resource.Properties()
    ) :
        this(url = url, mediaType = mediaType, properties = properties, { Try.success(bytes) })

    public constructor(bytes: ByteArray, link: Link)
        : this(bytes = bytes, url = Url(link.href), mediaType = link.mediaType, properties = link.properties.toResourceProperties())

    public constructor(link: Link, bytes: suspend () -> ResourceTry<ByteArray>)
        : this(url = Url(link.href), mediaType = link.mediaType, properties = link.properties.toResourceProperties(), bytes = bytes)

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() }} bytes)"
}

/** Creates a Resource serving a [String]. */
public class StringResource(
    url: Url? = null,
    mediaType: MediaType? = null,
    properties: Resource.Properties = Resource.Properties(),
    string: suspend () -> ResourceTry<String>
) : BaseBytesResource(url = url, mediaType = mediaType, properties = properties, { string().map { it.toByteArray() } }) {

    public constructor(
        string: String,
        url: Url? = null,
        mediaType: MediaType? = null,
        properties: Resource.Properties = Resource.Properties()
    ) :
        this(url = url, mediaType = mediaType, properties = properties, { Try.success(string) })

    public constructor(string: String, link: Link)
        : this(string = string, url = Url(link.href), mediaType = link.mediaType, properties = link.properties.toResourceProperties())

    public constructor(link: Link, string: suspend () -> ResourceTry<String>)
        : this(url = Url(link.href), mediaType = link.mediaType, properties = link.properties.toResourceProperties(), string = string)

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { readAsString() }})"
}
