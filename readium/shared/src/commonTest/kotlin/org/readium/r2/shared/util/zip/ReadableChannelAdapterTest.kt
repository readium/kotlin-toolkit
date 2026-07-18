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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.zip.jvm.ClosedChannelException
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * Characterization tests for [ReadableChannelAdapter], preserving the behavior of the legacy
 * blocking adapter.
 */
class ReadableChannelAdapterTest {

    private val data = testData(10)

    private fun channel(readable: TrackingReadable = TrackingReadable(data)) =
        ReadableChannelAdapter(readable, ::ReadException)

    @Test
    fun sequentialReadsAdvancePosition() = runTest {
        val readable = TrackingReadable(data)
        val channel = channel(readable)
        val buffer = ZipBuffer.allocate(4)

        assertEquals(4, channel.read(buffer))
        assertContentEquals(data.copyOfRange(0, 4), buffer.array())
        assertEquals(4, channel.position())

        buffer.clear()
        assertEquals(4, channel.read(buffer))
        assertContentEquals(data.copyOfRange(4, 8), buffer.array())
        assertEquals(8, channel.position())

        assertEquals(listOf(0L until 4L, 4L until 8L), readable.reads)
    }

    @Test
    fun readIsClampedToTheEndOfData() = runTest {
        val channel = channel()
        channel.position(8)
        val buffer = ZipBuffer.allocate(8)

        assertEquals(2, channel.read(buffer))
        assertEquals(2, buffer.position())
        assertContentEquals(data.copyOfRange(8, 10), buffer.array().copyOfRange(0, 2))
    }

    @Test
    fun readAtEndOfDataReturnsMinusOne() = runTest {
        val channel = channel()
        channel.position(10)
        assertEquals(-1, channel.read(ZipBuffer.allocate(4)))
    }

    @Test
    fun readFillsBufferFromItsCurrentPosition() = runTest {
        val channel = channel()
        val buffer = ZipBuffer.allocate(6)
        buffer.position(2)

        assertEquals(4, channel.read(buffer))
        assertEquals(6, buffer.position())
        assertContentEquals(data.copyOfRange(0, 4), buffer.array().copyOfRange(2, 6))
    }

    @Test
    fun readOnlyRequestsTheRangeItNeeds() = runTest {
        val readable = TrackingReadable(data)
        val channel = channel(readable)
        channel.position(5)

        channel.read(ZipBuffer.allocate(3))
        assertEquals(listOf(5L until 8L), readable.reads)
    }

    @Test
    fun sizeReturnsTheReadableLength() = runTest {
        assertEquals(10, channel().size())
    }

    @Test
    fun writeAndTruncateAreUnsupported() = runTest {
        val channel = channel()
        assertFailsWith<NonWritableChannelException> { channel.write(ZipBuffer.allocate(1)) }
        assertFailsWith<NonWritableChannelException> { channel.truncate(5) }
    }

    @Test
    fun closeClosesTheReadableAndForbidsReading() = runTest {
        val readable = TrackingReadable(data)
        val channel = channel(readable)

        assertTrue(channel.isOpen)
        channel.close()
        assertFalse(channel.isOpen)
        assertTrue(readable.isClosed)
        assertFailsWith<ClosedChannelException> { channel.read(ZipBuffer.allocate(1)) }
        assertFailsWith<ClosedChannelException> { channel.position(0) }
        assertFailsWith<ClosedChannelException> { channel.size() }
    }

    @Test
    fun negativePositionIsInvalid() = runTest {
        assertFailsWith<IllegalArgumentException> { channel().position(-1) }
    }
}
