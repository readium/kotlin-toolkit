/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getEquivalent

/**
 * Largest prefix kept in memory by [CachingReadable].
 *
 * The cache exists to make repeated head reads (format sniffing) cheap, so it only needs to cover
 * the sniffers' window. Without a cap, a whole-resource read of any size would be retained.
 */
private const val MAX_CACHED_PREFIX_SIZE: Int = 16 * 1024

internal class CachingReadable(
    private val source: Readable,
) : Readable by source {

    private var startCache: ByteArray? = null

    /** Retains [bytes] as the prefix cache, unless it is too large to be worth holding. */
    private fun cachePrefix(bytes: ByteArray) {
        startCache = bytes.takeIf { it.size <= MAX_CACHED_PREFIX_SIZE }
    }

    /**
     * Streaming bypasses the prefix cache and goes straight to the source: the cache exists to
     * serve small repeated head reads, not bulk transfers.
     *
     * Forwarding explicitly rather than through `by source`, so that this stays a deliberate
     * decision if the interface grows.
     */
    override suspend fun stream(
        range: LongRange?,
        consume: (ByteArray) -> Unit,
    ): Try<Unit, ReadError> =
        source.stream(range, consume)

    private var contentLength: Long? = null

    override suspend fun length(): Try<Long, ReadError> {
        contentLength?.let { return Try.success(it) }

        return source.length()
            .onSuccess { contentLength = it }
    }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
        return when {
            startCache == null -> {
                source.read(range)
                    .onSuccess {
                        if (range == null || range.first == 0L) {
                            cachePrefix(it)
                        }
                    }
            }
            range == null -> {
                if (contentLength == startCache!!.size.toLong()) {
                    Try.success(startCache!!)
                } else {
                    source.read()
                        .onSuccess {
                            cachePrefix(it)
                            contentLength = it.size.toLong()
                        }
                }
            }
            range.first == 0L -> {
                if (range.last < startCache!!.size) {
                    Try.success(startCache!!.sliceArray(0..range.last.toInt()))
                } else {
                    source.read(range)
                        .onSuccess { cachePrefix(it) }
                }
            }
            else ->
                return source.read(range)
        }
    }

    override fun close() {}
}

internal class CachingContainer(
    private val container: Container<Readable>,
) : Container<Readable> by container {

    private val cache: MutableMap<Url, CachingReadable> =
        mutableMapOf()

    override fun get(url: Url): Readable? {
        cache.getEquivalent(url)?.let { return it }

        val entry = container[url]
            ?: return null

        val blobContext = CachingReadable(entry)

        cache[url] = blobContext

        return blobContext
    }

    override fun close() {
        cache.forEach { it.value.close() }
        cache.clear()
    }
}
