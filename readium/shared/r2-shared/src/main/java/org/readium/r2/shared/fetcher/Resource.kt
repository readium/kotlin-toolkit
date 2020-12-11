/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import org.readium.r2.shared.R
import org.readium.r2.shared.UserException
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.Charset

typealias ResourceTry<SuccessT> = Try<SuccessT, Resource.Exception>

/**
 * Implements the transformation of a Resource. It can be used, for example, to decrypt,
 * deobfuscate, inject CSS or JavaScript, correct content – e.g. adding a missing dir="rtl" in an
 * HTML document, pre-process – e.g. before indexing a publication's content, etc.
 *
 * If the transformation doesn't apply, simply return resource unchanged.
 */
typealias ResourceTransformer = (Resource) -> Resource

/**
 * Acts as a proxy to an actual resource by handling read access.
 */
interface Resource {

    /**
     * Direct file to this resource, when available.
     *
     * This is meant to be used as an optimization for consumers which can't work efficiently
     * with streams. However, [file] is not guaranteed to be set, for example if the resource
     * underwent transformations or is being read from an archive. Therefore, consumers should
     * always fallback on regular stream reading, using [read] or [ResourceInputStream].
     */
    val file: File? get() = null

    /**
     * Returns the link from which the resource was retrieved.
     *
     * It might be modified by the [Resource] to include additional metadata, e.g. the
     * `Content-Type` HTTP header in [Link.type].
     */
    suspend fun link(): Link

    /**
     * Returns data length from metadata if available, or calculated from reading the bytes otherwise.
     *
     * This value must be treated as a hint, as it might not reflect the actual bytes length. To get
     * the real length, you need to read the whole resource.
     */
    suspend fun length(): ResourceTry<Long>

    /**
     * Reads the bytes at the given range.
     *
     * When [range] is null, the whole content is returned. Out-of-range indexes are clamped to the
     * available length automatically.
     */
    suspend fun read(range: LongRange? = null): ResourceTry<ByteArray>

    /**
     * Reads the full content as a [String].
     *
     * If [charset] is null, then it is parsed from the `charset` parameter of link().type,
     * or falls back on UTF-8.
     */
    suspend fun readAsString(charset: Charset? = null): ResourceTry<String> =
        read().mapCatching {
            String(it, charset = charset ?: link().mediaType.charset ?: Charsets.UTF_8)
        }

    /**
     * Reads the full content as a JSON object.
     */
    suspend fun readAsJson(): ResourceTry<JSONObject> =
        readAsString(charset = Charsets.UTF_8).mapCatching { JSONObject(it) }

    /**
     * Reads the full content as an XML document.
     */
    suspend fun readAsXml(): ResourceTry<ElementNode> =
        read().mapCatching { XmlParser().parse(ByteArrayInputStream(it)) }

    /**
     * Closes any opened file handles.
     *
     * If the Resource is already closed then invoking this method has no effect.
     */
    suspend fun close()

    /**
     * Executes the given block function on this resource and then closes it down correctly whether an exception is thrown or not.
     */
    suspend fun <R> use(block: suspend (Resource) -> R): R {
        var exception: Throwable? = null
        try {
            return block(this)
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            if (exception == null)
                close()
            else
                try {
                    close()
                } catch (closeException: Throwable) {
                    exception.addSuppressed(closeException)
                }

        }
    }

    companion object {
        /**
         * Creates a cached resource wrapping this resource.
         */
        fun Resource.cached(): Resource =
            if (this is CachingResource) this
            else CachingResource(this)
    }

    /**
     * Errors occurring while accessing a resource.
     */
    sealed class Exception(@StringRes userMessageId: Int, cause: Throwable? = null) : UserException(userMessageId, cause = cause) {

        /** Equivalent to a 400 HTTP error. */
        class BadRequest(val parameters: Map<String, String>, cause: Throwable? = null)
            : Exception(R.string.r2_shared_resource_exception_bad_request, cause)

        /** Equivalent to a 404 HTTP error. */
        object NotFound : Exception(R.string.r2_shared_resource_exception_not_found)

        /**
         * Equivalent to a 403 HTTP error.
         *
         * This can be returned when trying to read a resource protected with a DRM that is not
         * unlocked.
         */
        object Forbidden : Exception(R.string.r2_shared_resource_exception_forbidden)

        /**
         * Equivalent to a 503 HTTP error.
         *
         * Used when the source can't be reached, e.g. no Internet connection, or an issue with the
         * file system. Usually this is a temporary error.
         */
        object Unavailable : Exception(R.string.r2_shared_resource_exception_unavailable)

        /**
         * Equivalent to a 507 HTTP error.
         *
         * Used when the requested range is too large to be read in memory.
         */
        class OutOfMemory(cause: OutOfMemoryError)
            : Exception(R.string.r2_shared_resource_exception_out_of_memory, cause)

        /**
         * The request was cancelled by the caller.
         *
         * For example, when a coroutine is cancelled.
         */
        object Cancelled : Exception(R.string.r2_shared_resource_exception_cancelled)

        /** For any other error, such as HTTP 500. */
        class Other(cause: Throwable) : Exception(R.string.r2_shared_resource_exception_other, cause)

        companion object {

            fun wrap(e: Throwable): Exception =
                when (e) {
                    is Resource.Exception -> e
                    is CancellationException -> Cancelled
                    is OutOfMemoryError -> OutOfMemory(e)
                    else -> Other(e)
                }

        }

    }
}

/** Creates a Resource that will always return the given [error]. */
class FailureResource(private val link: Link, private val error: Resource.Exception) : Resource {

    internal constructor(link: Link, cause: Throwable) : this(link, Resource.Exception.Other(cause))

    override suspend fun link(): Link = link

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = Try.failure(error)

    override suspend fun length():  ResourceTry<Long> = Try.failure(error)

    override suspend fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}(${error})"
            
}

/**
 * A base class for a [Resource] which acts as a proxy to another one.
 *
 * Every function is delegating to the proxied resource, and subclasses should override some of them.
 */
abstract class ProxyResource(protected val resource: Resource) : Resource {

    override suspend fun link(): Link = resource.link()

    override suspend fun length(): ResourceTry<Long> = resource.length()

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = resource.read(range)

    override suspend fun close() = resource.close()

    override fun toString(): String =
        "${javaClass.simpleName}($resource)"

}

/**
 * Caches the members of [resource] on first access, to optimize subsequent accesses.
 *
 * This can be useful when reading [resource] is expensive.
 *
 * Warning: bytes are read and cached entirely the first time, even if only a [range] is requested.
 * So this is not appropriate for large resources.
 */
class CachingResource(protected val resource: Resource) : Resource {

    private lateinit var _link: Link
    private lateinit var _length: ResourceTry<Long>
    private lateinit var _bytes: ResourceTry<ByteArray>

    override suspend fun link(): Link {
        if (!::_link.isInitialized) {
            _link = resource.link()
        }
        return _link
    }

    override suspend fun length(): ResourceTry<Long> {
        if (!::_length.isInitialized) {
            _length = if (::_bytes.isInitialized) _bytes.map { it.size.toLong() } else resource.length()
        }
        return _length
    }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        if (!::_bytes.isInitialized) {
            _bytes = resource.read()
        }

        if (range == null)
            return _bytes

        return _bytes.map {
            @Suppress("NAME_SHADOWING")
            val range = range
                .coerceIn(0L.. it.size)
                .requireLengthFitInt()

            it.sliceArray(range.map(Long::toInt)) }
    }

    override suspend fun close() = resource.close()

    override fun toString(): String =
        "${javaClass.simpleName}($resource)"
}

/**
 * Transforms the bytes of [resource] on-the-fly.
 *
 * Warning: The transformation runs on the full content of [resource], so it's not appropriate for
 * large resources which can't be held in memory. Also, wrapping a [TransformingResource] in a
 * [CachingResource] can be a good idea to cache the result of the transformation in case multiple
 * ranges will be read.
 */
abstract class TransformingResource(resource: Resource) : ProxyResource(resource) {

    abstract suspend fun transform(data: ResourceTry<ByteArray>):  ResourceTry<ByteArray>

    private suspend fun bytes(): ResourceTry<ByteArray> =
        transform(resource.read())

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

/**
 * Wraps a [Resource] which will be created only when first accessing one of its members.
 */
class LazyResource(private val factory: suspend () -> Resource) : Resource {

    private lateinit var _resource: Resource

    private suspend fun resource(): Resource {
        if (!::_resource.isInitialized)
            _resource = factory()

        return _resource
    }

    override suspend fun link(): Link = resource().link()

    override suspend fun length(): ResourceTry<Long> = resource().length()

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = resource().read(range)

    override suspend fun close() {
        if (::_resource.isInitialized)
            _resource.close()
    }

    override fun toString(): String =
        if (::_resource.isInitialized) {
            "${javaClass.simpleName}($_resource)"
        } else {
            "${javaClass.simpleName}(...)"
        }
    
}

/**
 * Maps the result with the given [transform]
 *
 * If the [transform] throws an [Exception], it is wrapped in a failure with Resource.Exception.Other.
 */
inline fun <R, S> ResourceTry<S>.mapCatching(transform: (value: S) -> R): ResourceTry<R> =
    try {
        Try.success((transform(getOrThrow())))
    } catch (e: Exception) {
        Try.failure(Resource.Exception.wrap(e))
    } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
        Try.failure(Resource.Exception.wrap(e))
    }

inline fun <R, S> ResourceTry<S>.flatMapCatching(transform: (value: S) -> ResourceTry<R>): ResourceTry<R> =
    mapCatching(transform).flatMap { it }
