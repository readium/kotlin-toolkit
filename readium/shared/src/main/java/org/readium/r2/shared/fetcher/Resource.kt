/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.fetcher

import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.extensions.contains
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry

/**
 * Implements the transformation of a Resource. It can be used, for example, to decrypt,
 * deobfuscate, inject CSS or JavaScript, correct content – e.g. adding a missing dir="rtl" in an
 * HTML document, pre-process – e.g. before indexing a publication's content, etc.
 *
 * If the transformation doesn't apply, simply return resource unchanged.
 */
typealias ResourceTransformer = (Fetcher.Resource) -> Fetcher.Resource

/** Creates a Resource that will always return the given [error]. */
class FailureResource(private val link: Link, private val error: Resource.Exception) :
    Fetcher.Resource {

    internal constructor(link: Link, cause: Throwable) : this(link, Resource.Exception.Other(cause))

    override suspend fun link(): Link = link

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = Try.failure(error)

    override suspend fun length(): ResourceTry<Long> = Try.failure(error)

    override suspend fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}($error)"
}

/**
 * Resource that will act as a proxy to a fallback resource if the [originalResource] errors out.
 */
class FallbackResource(
    private val originalResource: Fetcher.Resource,
    private val fallbackResourceFactory: (Resource.Exception) -> Fetcher.Resource
) : Fetcher.Resource {
    private val coroutineScope =
        CoroutineScope(Dispatchers.Default)

    private val resource: Deferred<Fetcher.Resource> =
        coroutineScope.async {
            when (val result = originalResource.length()) {
                is Try.Success -> originalResource
                is Try.Failure -> fallbackResourceFactory(result.value)
            }
    }

    override suspend fun link(): Link =
        resource.await().link()

    override suspend fun length(): ResourceTry<Long> =
        resource.await().length()

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        resource.await().read(range)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun close() {
        coroutineScope.cancel()
        if (resource.isCompleted) {
            resource.getCompleted().close()
        }
    }
}

/**
 * Falls back to alternative resources when the receiver fails.
 */
fun Fetcher.Resource.fallback(fallbackResourceFactory: (Resource.Exception) -> Fetcher.Resource): Fetcher.Resource =
    FallbackResource(this, fallbackResourceFactory)

/**
 * Falls back to the given alternative [Fetcher.Resource] when the receiver fails.
 */
fun Fetcher.Resource.fallback(fallbackResource: Fetcher.Resource): Fetcher.Resource =
    FallbackResource(this) { fallbackResource }

/**
 * A base class for a [Resource] which acts as a proxy to another one.
 *
 * Every function is delegating to the proxied resource, and subclasses should override some of them.
 */
abstract class ProxyResource(protected val resource: Fetcher.Resource) : Fetcher.Resource {

    override val file: File? = resource.file

    override suspend fun link(): Link = resource.link()

    override suspend fun length(): ResourceTry<Long> = resource.length()

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = resource.read(range)

    override suspend fun close() = resource.close()

    override fun toString(): String =
        "${javaClass.simpleName}($resource)"
}

/**
 * Transforms the bytes of [resource] on-the-fly.
 *
 * If you set [cacheBytes] to false, consider providing your own implementation of [length] to avoid
 * unnecessary transformations.
 *
 * Warning: The transformation runs on the full content of [resource], so it's not appropriate for
 * large resources which can't be held in memory.
 */
abstract class TransformingResource(
    resource: Fetcher.Resource,
    private val cacheBytes: Boolean = true
) : ProxyResource(resource) {

    companion object {
        /**
         * Creates a [TransformingResource] using the given [transform] function.
         */
        operator fun invoke(resource: Fetcher.Resource, transform: suspend (ByteArray) -> ByteArray): TransformingResource =
            object : TransformingResource(resource) {
                override suspend fun transform(data: ResourceTry<ByteArray>): ResourceTry<ByteArray> =
                    data.mapCatching { transform(it) }
            }
    }

    private lateinit var _bytes: ResourceTry<ByteArray>

    abstract suspend fun transform(data: ResourceTry<ByteArray>): ResourceTry<ByteArray>

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

/**
 * Wraps a [Fetcher.Resource] which will be created only when first accessing one of its members.
 */
class LazyResource(private val factory: suspend () -> Fetcher.Resource) : Fetcher.Resource {

    private lateinit var _resource: Fetcher.Resource

    private suspend fun resource(): Fetcher.Resource {
        if (!::_resource.isInitialized)
            _resource = factory()

        return _resource
    }

    override suspend fun link(): Link =
        resource().link()

    override suspend fun length(): ResourceTry<Long> =
        resource().length()

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        resource().read(range)

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
 * Protects the access to a wrapped resource with a mutex to make it thread-safe.
 *
 * This doesn't implement [ProxyResource] to avoid forgetting the synchronization for a future API.
 */
class SynchronizedResource(
    private val resource: Fetcher.Resource
) : Fetcher.Resource {

    private val mutex = Mutex()

    override val file: File? =
        resource.file

    override suspend fun link(): Link =
        mutex.withLock { resource.link() }

    override suspend fun length(): ResourceTry<Long> =
        mutex.withLock { resource.length() }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        mutex.withLock { resource.read(range) }

    override suspend fun close() =
        mutex.withLock { resource.close() }

    override fun toString(): String =
        "${javaClass.simpleName}($resource)"
}

/**
 * Wraps this resource in a [SynchronizedResource] to protect the access from multiple threads.
 */
fun Fetcher.Resource.synchronized(): SynchronizedResource =
    SynchronizedResource(this)

/**
 * Wraps a [Resource] and buffers its content.
 *
 * Expensive interaction with the underlying resource is minimized, since most (smaller) requests
 * can be satisfied by accessing the buffer alone. The drawback is that some extra space is required
 * to hold the buffer and that copying takes place when filling that buffer, but this is usually
 * outweighed by the performance benefits.
 *
 * Note that this implementation is pretty limited and the benefits are only apparent when reading
 * forward and consecutively – e.g. when downloading the resource by chunks. The buffer is ignored
 * when reading backward or far ahead.
 *
 * @param resource Underlying resource which will be buffered.
 * @param resourceLength The total length of the resource, when known. This can improve performance
 *        by avoiding requesting the length from the underlying resource.
 * @param bufferSize Size of the buffer chunks to read.
 */
class BufferingResource(
    resource: Fetcher.Resource,
    resourceLength: Long? = null,
    private val bufferSize: Long = DEFAULT_BUFFER_SIZE,
) : ProxyResource(resource) {

    companion object {
        const val DEFAULT_BUFFER_SIZE: Long = 8192
    }

    /**
     * The buffer containing the current bytes read from the wrapped [Resource], with the range it
     * covers.
     */
    private var buffer: Pair<ByteArray, LongRange>? = null

    private lateinit var _cachedLength: ResourceTry<Long>
    private suspend fun cachedLength(): ResourceTry<Long> {
        if (!::_cachedLength.isInitialized)
            _cachedLength = resource.length()
        return _cachedLength
    }

    init {
        if (resourceLength != null) {
            _cachedLength = Try.success(resourceLength)
        }
    }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
        val length = cachedLength().successOrNull()
        // Reading the whole resource bypasses buffering to keep things simple.
        if (range == null || length == null) {
            return super.read(range)
        }

        val requestedRange = range
            .coerceIn(0L until length)
            .requireLengthFitInt()
        if (requestedRange.isEmpty()) {
            return Try.success(ByteArray(0))
        }

        // Round up the range to be read to the next `bufferSize`, because we will buffer the
        // excess.
        val readLast = (requestedRange.last + 1).ceilMultipleOf(bufferSize).coerceAtMost(length)
        var readRange = requestedRange.first until readLast

        // Attempt to serve parts or all of the request using the buffer.
        buffer?.let { pair ->
            var (buffer, bufferedRange) = pair

            // Everything already buffered?
            if (bufferedRange.contains(requestedRange)) {
                val data = extractRange(requestedRange, buffer, start = bufferedRange.first)
                return Try.success(data)

                // Beginning of requested data is buffered?
            } else if (bufferedRange.contains(requestedRange.first)) {
                readRange = (bufferedRange.last + 1)..readRange.last

                return super.read(readRange).map { readData ->
                    buffer += readData
                    // Shift the current buffer to the tail of the read data.
                    saveBuffer(buffer, readRange)

                    val bytes = extractRange(requestedRange, buffer, start = bufferedRange.first)
                    bytes
                }
            }
        }

        // Fallback on reading the requested range from the original resource.
        return super.read(readRange).map { data ->
            saveBuffer(data, readRange)

            val res = if (data.count() > requestedRange.count())
                data.copyOfRange(0, requestedRange.count())
            else
                data

            res
        }
    }

    /**
     * Keeps the last chunk of the given data as the buffer for next reads.
     *
     * @param data Data read from the original resource.
     * @param range Range of the read data in the resource.
     */
    private fun saveBuffer(data: ByteArray, range: LongRange) {
        val lastChunk = data.takeLast(bufferSize.toInt()).toByteArray()
        val chunkRange = (range.last + 1 - lastChunk.count())..range.last
        buffer = Pair(lastChunk, chunkRange)
    }

    /**
     * Reads a sub-range of the given [data] after shifting the given absolute (to the resource)
     * ranges to be relative to [data].
     */
    private fun extractRange(requestedRange: LongRange, data: ByteArray, start: Long): ByteArray {
        val first = requestedRange.first - start
        val lastExclusive = first + requestedRange.count()
        require(first >= 0)
        require(lastExclusive <= data.count()) { "$lastExclusive > ${data.count()}" }
        return data.copyOfRange(first.toInt(), lastExclusive.toInt())
    }

    private fun Long.ceilMultipleOf(divisor: Long) =
        divisor * (this / divisor + if (this % divisor == 0L) 0 else 1)
}

/**
 * Wraps this resource in a [BufferingResource] to improve reading performances.
 *
 * @param resourceLength The total length of the resource, when known. This can improve performance
 *        by avoiding requesting the length from the underlying resource.
 * @param size Size of the buffer chunks to read.
 */
fun Fetcher.Resource.buffered(
    resourceLength: Long? = null,
    size: Long = BufferingResource.DEFAULT_BUFFER_SIZE
) =
    BufferingResource(resource = this, resourceLength = resourceLength, bufferSize = size)

/**
 * Maps the result with the given [transform]
 *
 * If the [transform] throws an [Exception], it is wrapped in a failure with Resource.Exception.Other.
 */
inline fun <R, S> ResourceTry<S>.mapCatching(transform: (value: S) -> R): ResourceTry<R> =
    try {
        map(transform)
    } catch (e: Exception) {
        Try.failure(Resource.Exception.wrap(e))
    } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
        Try.failure(Resource.Exception.wrap(e))
    }

inline fun <R, S> ResourceTry<S>.flatMapCatching(transform: (value: S) -> ResourceTry<R>): ResourceTry<R> =
    mapCatching(transform).flatMap { it }
