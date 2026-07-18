/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.utils

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteUtilsTest {

    @Test
    fun fromLittleEndian() {
        assertEquals(0L, ByteUtils.fromLittleEndian(ByteArray(0)))
        assertEquals(
            0x0807060504030201L,
            ByteUtils.fromLittleEndian(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        )
        assertEquals(0x0302L, ByteUtils.fromLittleEndian(byteArrayOf(1, 2, 3, 4), 1, 2))
        // Unsigned bytes.
        assertEquals(0xFFL, ByteUtils.fromLittleEndian(byteArrayOf(-1), 0, 1))
    }

    @Test
    fun fromLittleEndianRejectsMoreThanEightBytes() {
        assertFailsWith<IllegalArgumentException> {
            ByteUtils.fromLittleEndian(ByteArray(9))
        }
    }

    @Test
    fun toLittleEndian() {
        val buf = ByteArray(4)
        ByteUtils.toLittleEndian(buf, 0x04030201L, 0, 4)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), buf)
    }

    @Test
    fun roundTrip() {
        val buf = ByteArray(8)
        ByteUtils.toLittleEndian(buf, -1L, 0, 8)
        assertEquals(-1L, ByteUtils.fromLittleEndian(buf))
    }
}

class Crc32Test {

    @Test
    fun knownValues() {
        // CRC-32 of "123456789" is 0xCBF43926.
        val crc = Crc32()
        crc.update("123456789".encodeToByteArray())
        assertEquals(0xCBF43926L, crc.value)
    }

    @Test
    fun emptyInput() {
        assertEquals(0L, Crc32().value)
    }

    @Test
    fun incrementalUpdates() {
        val crc = Crc32()
        val bytes = "123456789".encodeToByteArray()
        crc.update(bytes, 0, 4)
        crc.update(bytes, 4, 5)
        assertEquals(0xCBF43926L, crc.value)

        crc.reset()
        crc.update(bytes)
        assertEquals(0xCBF43926L, crc.value)
    }
}
