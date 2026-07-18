/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.jvm

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ZipBufferTest {

    @Test
    fun allocateInitializesPositionAndLimit() {
        val buffer = ZipBuffer.allocate(8)
        assertEquals(0, buffer.position())
        assertEquals(8, buffer.limit())
        assertEquals(8, buffer.capacity())
        assertEquals(8, buffer.remaining())
        assertTrue(buffer.hasRemaining())
    }

    @Test
    fun wrapUsesTheGivenArrayAsBackingArray() {
        val array = byteArrayOf(1, 2, 3, 4)
        val buffer = ZipBuffer.wrap(array)
        assertSame(array, buffer.array())
        assertEquals(0, buffer.arrayOffset())
        assertEquals(0, buffer.position())
        assertEquals(4, buffer.limit())
        assertEquals(4, buffer.capacity())
    }

    @Test
    fun wrapWithOffsetSetsPositionAndLimitButKeepsFullCapacity() {
        val array = ByteArray(10)
        val buffer = ZipBuffer.wrap(array, 2, 5)
        assertEquals(2, buffer.position())
        assertEquals(7, buffer.limit())
        assertEquals(10, buffer.capacity())
        assertEquals(5, buffer.remaining())
    }

    @Test
    fun wrapWithInvalidRangeFails() {
        assertFailsWith<IndexOutOfBoundsException> { ZipBuffer.wrap(ByteArray(4), 2, 3) }
        assertFailsWith<IndexOutOfBoundsException> { ZipBuffer.wrap(ByteArray(4), -1, 2) }
    }

    @Test
    fun relativeGetAdvancesPosition() {
        val buffer = ZipBuffer.wrap(byteArrayOf(10, 20, 30))
        assertEquals(10, buffer.get().toInt())
        assertEquals(20, buffer.get().toInt())
        assertEquals(2, buffer.position())
        assertEquals(30, buffer.get().toInt())
        assertFalse(buffer.hasRemaining())
        assertFailsWith<IndexOutOfBoundsException> { buffer.get() }
    }

    @Test
    fun bulkGetCopiesAndAdvancesPosition() {
        val buffer = ZipBuffer.wrap(byteArrayOf(1, 2, 3, 4, 5))
        val dst = ByteArray(4)
        buffer.get(dst, 1, 3)
        assertContentEquals(byteArrayOf(0, 1, 2, 3), dst)
        assertEquals(3, buffer.position())

        val rest = ByteArray(2)
        buffer.get(rest)
        assertContentEquals(byteArrayOf(4, 5), rest)
        assertEquals(5, buffer.position())
    }

    @Test
    fun bulkGetPastLimitFails() {
        val buffer = ZipBuffer.wrap(byteArrayOf(1, 2))
        assertFailsWith<IndexOutOfBoundsException> { buffer.get(ByteArray(3)) }
        // The buffer is unchanged on failure.
        assertEquals(0, buffer.position())
    }

    @Test
    fun relativePutAdvancesPosition() {
        val buffer = ZipBuffer.allocate(2)
        buffer.put(1).put(2)
        assertFailsWith<IndexOutOfBoundsException> { buffer.put(3) }
        assertContentEquals(byteArrayOf(1, 2), buffer.array())
    }

    @Test
    fun bulkPutCopiesAndAdvancesPosition() {
        val buffer = ZipBuffer.allocate(5)
        buffer.put(byteArrayOf(1, 2, 3))
        buffer.put(byteArrayOf(9, 4, 5, 9), 1, 2)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), buffer.array())
        assertEquals(5, buffer.position())
        assertFailsWith<IndexOutOfBoundsException> { buffer.put(byteArrayOf(6)) }
    }

    @Test
    fun putBufferCopiesRemainingBytesOfSource() {
        val src = ZipBuffer.wrap(byteArrayOf(1, 2, 3, 4))
        src.position(1)
        val dst = ZipBuffer.allocate(5)
        dst.put(42)
        dst.put(src)
        assertContentEquals(byteArrayOf(42, 2, 3, 4, 0), dst.array())
        assertEquals(4, dst.position())
        assertEquals(4, src.position())
        assertFalse(src.hasRemaining())
    }

    @Test
    fun putBufferOnItselfFails() {
        val buffer = ZipBuffer.wrap(byteArrayOf(1, 2, 3))
        assertFailsWith<IllegalArgumentException> { buffer.put(buffer) }
    }

    @Test
    fun putBufferPastLimitFails() {
        val src = ZipBuffer.wrap(byteArrayOf(1, 2, 3))
        val dst = ZipBuffer.allocate(2)
        assertFailsWith<IndexOutOfBoundsException> { dst.put(src) }
    }

    @Test
    fun flipSetsLimitToPositionAndRewinds() {
        val buffer = ZipBuffer.allocate(8)
        buffer.put(byteArrayOf(1, 2, 3))
        buffer.flip()
        assertEquals(0, buffer.position())
        assertEquals(3, buffer.limit())
        assertEquals(3, buffer.remaining())
    }

    @Test
    fun rewindKeepsLimit() {
        val buffer = ZipBuffer.wrap(byteArrayOf(1, 2, 3))
        buffer.limit(2)
        buffer.get()
        buffer.rewind()
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.limit())
    }

    @Test
    fun clearRestoresPositionAndLimit() {
        val buffer = ZipBuffer.wrap(byteArrayOf(1, 2, 3))
        buffer.limit(2)
        buffer.get()
        buffer.clear()
        assertEquals(0, buffer.position())
        assertEquals(3, buffer.limit())
    }

    @Test
    fun loweringLimitClampsPosition() {
        val buffer = ZipBuffer.allocate(8)
        buffer.position(6)
        buffer.limit(4)
        assertEquals(4, buffer.position())
        assertEquals(4, buffer.limit())
    }

    @Test
    fun invalidPositionOrLimitFails() {
        val buffer = ZipBuffer.allocate(4)
        assertFailsWith<IllegalArgumentException> { buffer.position(5) }
        assertFailsWith<IllegalArgumentException> { buffer.position(-1) }
        assertFailsWith<IllegalArgumentException> { buffer.limit(5) }
        assertFailsWith<IllegalArgumentException> { buffer.limit(-1) }
    }
}
