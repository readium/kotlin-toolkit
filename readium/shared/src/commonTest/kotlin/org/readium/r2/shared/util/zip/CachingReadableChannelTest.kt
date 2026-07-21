/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * Characterization tests for [CachingReadableChannel], preserving the read patterns of the legacy
 * blocking implementation: a single source read fills the cached tail, and reads within the tail
 * never touch the source again.
 */
class CachingReadableChannelTest {

    private val data = testData(100)

    private fun channels(cacheFrom: Long): Pair<CachingReadableChannel, TrackingReadable> {
        val readable = TrackingReadable(data)
        val inner = ReadableChannelAdapter(readable, ::ReadException)
        return Pair(CachingReadableChannel(inner, cacheFrom), readable)
    }

    @Test
    fun readsWithinTheTailHitTheSourceOnlyOnce() = runTest {
        val (channel, readable) = channels(cacheFrom = 90)
        channel.position(92)

        val buffer = ZipBuffer.allocate(4)
        assertEquals(4, channel.read(buffer))
        assertContentEquals(data.copyOfRange(92, 96), buffer.array())

        // The single source read is the tail fill.
        assertEquals(listOf(90L until 100L), readable.reads)

        // Subsequent reads within the tail are served from the cache.
        channel.position(90)
        repeat(3) { channel.read(ZipBuffer.allocate(3)) }
        assertEquals(listOf(90L until 100L), readable.reads)
    }

    @Test
    fun cachePreloadsTheTail() = runTest {
        val (channel, readable) = channels(cacheFrom = 90)

        channel.cache()
        assertEquals(listOf(90L until 100L), readable.reads)

        channel.position(95)
        val buffer = ZipBuffer.allocate(5)
        assertEquals(5, channel.read(buffer))
        assertContentEquals(data.copyOfRange(95, 100), buffer.array())
        assertEquals(listOf(90L until 100L), readable.reads)
    }

    @Test
    fun readsBeforeTheTailAreDelegatedToTheSource() = runTest {
        val (channel, readable) = channels(cacheFrom = 90)

        assertEquals(4, channel.read(ZipBuffer.allocate(4)))
        assertEquals(4, channel.read(ZipBuffer.allocate(4)))
        assertEquals(listOf(0L until 4L, 4L until 8L), readable.reads)
    }

    @Test
    fun cachedReadsAdvanceThePosition() = runTest {
        val (channel, _) = channels(cacheFrom = 90)
        channel.cache()
        channel.position(90)
        channel.read(ZipBuffer.allocate(5))
        assertEquals(95, channel.position())
    }

    @Test
    fun cachedReadsAreClampedToTheTailEnd() = runTest {
        val (channel, _) = channels(cacheFrom = 90)
        channel.position(98)
        val buffer = ZipBuffer.allocate(8)
        assertEquals(2, channel.read(buffer))
        assertContentEquals(data.copyOfRange(98, 100), buffer.array().copyOfRange(0, 2))
    }

    @Test
    fun aFailedTailFillIsNotServedAsCachedData() = runTest {
        val readable = TrackingReadable(data)
        var failNextRead = true
        // Injects on `stream()`, the primary member, and deliberately does *not* use
        // `Readable by readable`: delegation would generate a `read()` forwarding straight to
        // `readable`, so the injected failure would never be seen on the `read()` path.
        val failingOnce = object : Readable {
            override suspend fun length(): Try<Long, ReadError> =
                readable.length()

            override suspend fun stream(
                range: LongRange?,
                consume: (ByteArray) -> Unit,
            ): Try<Unit, ReadError> =
                if (failNextRead) {
                    failNextRead = false
                    Try.failure(ReadError.Decoding("network blip"))
                } else {
                    readable.stream(range, consume)
                }

            override fun close() {
                readable.close()
            }
        }
        val channel = CachingReadableChannel(
            ReadableChannelAdapter(failingOnce, ::ReadException),
            cacheFrom = 90
        )

        // The first read triggers a tail fill which fails.
        channel.position(92)
        assertFailsWith<ReadException> { channel.read(ZipBuffer.allocate(4)) }

        // The tail must not be considered cached: the next read refills it and serves real bytes.
        channel.position(92)
        val buffer = ZipBuffer.allocate(4)
        assertEquals(4, channel.read(buffer))
        assertContentEquals(data.copyOfRange(92, 96), buffer.array())
        assertEquals(listOf(90L until 100L), readable.reads)
    }

    @Test
    fun cachingFromZeroServesTheWholeContentFromASingleSourceRead() = runTest {
        val (channel, readable) = channels(cacheFrom = 0)

        val result = ByteArray(100)
        var position = 0
        while (position < 100) {
            // Seek before each read, like ZipFile does.
            channel.position(position.toLong())
            val buffer = ZipBuffer.allocate(7)
            val read = channel.read(buffer)
            buffer.flip()
            buffer.get(result, position, read)
            position += read
        }

        assertContentEquals(data, result)
        assertEquals(listOf(0L until 100L), readable.reads)
    }
}
