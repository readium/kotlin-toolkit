/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.json.JSONObject
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Acts as a proxy to an actual resource by handling read access.
 */
interface Resource {

    /**
     * The link from which the resource was retrieved.
     *
     * It might be modified by the [Resource] to include additional metadata, e.g. the
     * `Content-Type` HTTP header in [Link.type].
     */
    val link: Link

    /**
     * Data length from metadata if available, or calculated from reading the bytes otherwise.
     *
     * This value must be treated as a hint, as it might not reflect the actual bytes length. To get
     * the real length, you need to read the whole resource.
     */
    val length: ResourceTry<Long>

    /**
     * Reads the bytes at the given range.
     *
     * When [range] is null, the whole content is returned. Out-of-range indexes are clamped to the
     * available length automatically.
     */
    fun read(range: LongRange? = null): ResourceTry<ByteArray>

    /**
     * Reads the full content as a [String].
     *
     * If [charset] is null, then it is parsed from the `charset` parameter of `link.type`, or falls
     * back on UTF-8.
     */
    fun readAsString(charset: Charset? = null): ResourceTry<String> =
        read().tryMap {
            String(it, charset = charset ?: link.mediaType?.charset ?: Charsets.UTF_8)
        }

    /**
     * Reads the full content as a JSON object.
     */
    fun readAsJson(): ResourceTry<JSONObject> =
        readAsString(charset = Charsets.UTF_8).tryMap { JSONObject(it) }

    /**
     * Reads the full content as an XML document.
     */
    fun readAsXml(): ResourceTry<ElementNode> =
        stream().tryMap { XmlParser().parse(it) }

    /**
     * Creates an [InputStream] to read the content.
     */
    fun stream(): ResourceTry<InputStream> =
        length.map { ResourceInputStream(resource = this, length = it) }

    /**
     * Closes any opened file handles.
     */
    fun close()

    /**
     * Errors occurring while accessing a resource.
     */
    sealed class Error(cause: Throwable? = null) : Throwable(cause) {

        /** Equivalent to a 404 HTTP error. */
        object NotFound : Error()

        /**
         * Equivalent to a 403 HTTP error.
         *
         * This can be returned when trying to read a resource protected with a DRM that is not unlocked.
         */
        object Forbidden : Error()

        /**
         * Equivalent to a 503 HTTP error.
         *
         * Used when the source can't be reached, e.g. no Internet connection, or an issue with the
         * file system. Usually this is a temporary error.
         */
        object Unavailable : Error()

        /** For any other error, such as HTTP 500. */
        class Other(cause: Throwable) : Error(cause)
    }
}

typealias ResourceTry<SuccessT> = Try<SuccessT, Resource.Error>

/**
 * Implements the transformation of a Resource. It can be used, for example, to decrypt,
 * deobfuscate, inject CSS or JavaScript, correct content – e.g. adding a missing dir="rtl" in an
 * HTML document, pre-process – e.g. before indexing a publication's content, etc.
 *
 * If the transformation doesn't apply, simply return resource unchanged.
 */
typealias ResourceTransformer = (Resource) -> Resource

/** Creates a Resource that will always return the given [error]. */
class FailureResource(override val link: Link, private val error: Resource.Error) : Resource {

    internal constructor(link: Link, cause: Throwable) : this(link, Resource.Error.Other(cause))

    override fun read(range: LongRange?): ResourceTry<ByteArray> = Try.failure(error)

    override val length:  ResourceTry<Long> = Try.failure(error)

    override fun close() {}
}

/** Creates a Resource serving an array of [bytes]. */
open class BytesResource(override val link: Link, private val bytes: () -> ByteArray) : Resource {
    private val byteArray by lazy(bytes)

    override fun read(range: LongRange?): ResourceTry<ByteArray> {
        if (range == null)
            return Try.success(byteArray)

        @Suppress("NAME_SHADOWING")
        val range = checkedRange(range)
        val byteRange = byteArray.sliceArray(range.map(Long::toInt))
        return Try.success(byteRange)
    }

    override val length: ResourceTry<Long> = Try.success(byteArray.size.toLong())

    override fun close() {}
}

/** Creates a Resource serving a string encoded as UTF-8. */
class StringResource(link: Link, string: () -> String) : BytesResource(link, { string().toByteArray() })

/**
 * A base class for a [Resource] which acts as a proxy to another one.
 *
 * Every function is delegating to the proxied resource, and subclasses should override some of them.
 */
internal abstract class ResourceProxy(
    protected val resource: Resource
) : Resource {

    override val link: Link get() = resource.link

    override val length: ResourceTry<Long> get() = resource.length

    override fun read(range: LongRange?): ResourceTry<ByteArray> {
        return resource.read(range)
    }

    override fun close() = resource.close()

}

internal abstract class StreamResource : Resource {

    abstract override fun stream(): ResourceTry<InputStream>

    /** An estimate of data length from metadata */
    protected abstract val metadataLength: Long?

    override fun read(range: LongRange?): ResourceTry<ByteArray> =
        if (range == null)
            readFully()
        else
            readRange(range)

    private fun readFully(): ResourceTry<ByteArray> =
        stream().tryMap { stream ->
            stream.use { it.readBytes() }
        }

    private fun readRange(range: LongRange): ResourceTry<ByteArray> =
        stream().tryMap { stream ->
            @Suppress("NAME_SHADOWING")
            val range = checkedRange(range)

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

    override val length: ResourceTry<Long>
        get() =
            metadataLength?.let { Try.success(it) }
                ?: readFully().map { it.size.toLong() }
}

/**
 * Maps the result with the given [transform]
 *
 * If the [transform] throws an [Exception], it is wrapped in a failure with Resource.Error.Other.
 */
fun <R, S> ResourceTry<S>.tryMap(transform: (value: S) -> R): ResourceTry<R> =
    try {
        Try.success((transform(getOrThrow())))
    } catch (e: Resource.Error) {
        Try.failure(e)
    } catch (e: Exception) {
        Try.failure(Resource.Error.Other(e))
    }

fun <R, S> ResourceTry<S>.tryFlatMap(transform: (value: S) -> ResourceTry<R>): ResourceTry<R> =
    tryMap(transform).flatMap { it }

private fun checkedRange(range: LongRange): LongRange =
    if (range.first >= range.last)
        0 until 0L
    else if (range.last - range.first + 1 > Int.MAX_VALUE)
        throw IllegalArgumentException("Range length greater than Int.MAX_VALUE")
    else
        LongRange(range.first.coerceAtLeast(0), range.last)
