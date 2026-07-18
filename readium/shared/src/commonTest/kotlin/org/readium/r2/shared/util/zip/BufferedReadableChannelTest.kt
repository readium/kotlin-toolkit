/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * Characterization tests for [BufferedReadableChannel], preserving the read patterns of the legacy
 * blocking implementation: the source is read by chunks of `bufferSize` bytes, and small
 * sequential reads are served from the buffer.
 */
class BufferedReadableChannelTest {

    private val data = testData(100)

    private fun channels(bufferSize: Int = 16): Pair<BufferedReadableChannel, TrackingReadable> {
        val readable = TrackingReadable(data)
        val inner = ReadableChannelAdapter(readable, ::ReadException)
        return Pair(BufferedReadableChannel(inner, bufferSize), readable)
    }

    @Test
    fun smallSequentialReadsAreBatchedIntoBufferSizedSourceReads() = runTest {
        val (channel, readable) = channels(bufferSize = 16)

        repeat(4) { index ->
            val buffer = ZipBuffer.allocate(4)
            assertEquals(4, channel.read(buffer))
            assertContentEquals(data.copyOfRange(index * 4, index * 4 + 4), buffer.array())
        }
        assertEquals(listOf(0L until 16L), readable.reads)

        // The 5th read exhausts the buffer and triggers a second chunk.
        assertEquals(4, channel.read(ZipBuffer.allocate(4)))
        assertEquals(listOf(0L until 16L, 16L until 32L), readable.reads)
    }

    @Test
    fun readsLargerThanTheBufferAreServedAcrossChunks() = runTest {
        val (channel, readable) = channels(bufferSize = 16)

        val buffer = ZipBuffer.allocate(24)
        assertEquals(24, channel.read(buffer))
        assertContentEquals(data.copyOfRange(0, 24), buffer.array())
        assertEquals(listOf(0L until 16L, 16L until 32L), readable.reads)
    }

    @Test
    fun wholeContentIsReadCorrectlyInSmallChunks() = runTest {
        val (channel, readable) = channels(bufferSize = 16)

        val result = ByteArray(100)
        var position = 0
        while (position < 100) {
            val buffer = ZipBuffer.allocate(7)
            val read = channel.read(buffer)
            buffer.flip()
            buffer.get(result, position, read)
            position += read
        }

        assertContentEquals(data, result)
        // 100 bytes read as 6 chunks of 16 then one of 4.
        assertEquals(7, readable.reads.size)
        assertEquals(96L until 100L, readable.reads.last())
    }

    @Test
    fun readAtEndOfDataReturnsZero() = runTest {
        val (channel, _) = channels()
        channel.position(100)
        assertEquals(0, channel.read(ZipBuffer.allocate(4)))
    }

    @Test
    fun positionAccountsForBufferedBytes() = runTest {
        val (channel, _) = channels(bufferSize = 16)
        channel.read(ZipBuffer.allocate(4))
        // The source is at 16, but 12 buffered bytes are still unread.
        assertEquals(4, channel.position())
    }

    @Test
    fun repositioningWithinTheBufferAvoidsSourceReads() = runTest {
        val (channel, readable) = channels(bufferSize = 16)
        channel.read(ZipBuffer.allocate(4))

        channel.position(0)
        val buffer = ZipBuffer.allocate(4)
        assertEquals(4, channel.read(buffer))
        assertContentEquals(data.copyOfRange(0, 4), buffer.array())
        assertEquals(listOf(0L until 16L), readable.reads)
    }

    @Test
    fun repositioningOutsideTheBufferInvalidatesIt() = runTest {
        val (channel, readable) = channels(bufferSize = 16)
        channel.read(ZipBuffer.allocate(4))

        channel.position(50)
        val buffer = ZipBuffer.allocate(4)
        assertEquals(4, channel.read(buffer))
        assertContentEquals(data.copyOfRange(50, 54), buffer.array())
        assertEquals(listOf(0L until 16L, 50L until 66L), readable.reads)
    }
}
