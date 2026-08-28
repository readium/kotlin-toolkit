/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getEquivalent

internal class CachingReadable(
    private val source: Readable,
) : Readable by source {

    private var startCache: ByteArray? = null

    private var contentLength: Long? = null

    override suspend fun length(): Try<Long, ReadError> {
        contentLength?.let { Try.success(it) }

        return source.length()
            .onSuccess { contentLength = it }
    }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
        return when {
            startCache == null -> {
                source.read(range)
                    .onSuccess {
                        if (range == null || range.first == 0L) {
                            startCache = it
                        }
                    }
            }
            range == null -> {
                if (contentLength == startCache!!.size.toLong()) {
                    Try.success(startCache!!)
                } else {
                    source.read()
                        .onSuccess {
                            startCache = it
                            contentLength = it.size.toLong()
                        }
                }
            }
            range.first == 0L -> {
                if (range.last < startCache!!.size) {
                    Try.success(startCache!!.sliceArray(0..range.last.toInt()))
                } else {
                    source.read(range)
                        .onSuccess { startCache = it }
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
