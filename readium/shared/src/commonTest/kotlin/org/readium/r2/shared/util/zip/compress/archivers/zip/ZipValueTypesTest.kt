/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZipShortTest {

    @Test
    fun toBytes() {
        val zs = ZipShort(0x1234)
        assertContentEquals(byteArrayOf(0x34, 0x12), zs.bytes)
    }

    @Test
    fun fromBytes() {
        assertEquals(0x1234, ZipShort(byteArrayOf(0x34, 0x12)).value)
        assertEquals(0xFFFF, ZipShort(byteArrayOf(-1, -1)).value)
    }

    @Test
    fun roundTripAtOffset() {
        val buf = ByteArray(4)
        ZipShort.putShort(0xABCD, buf, 1)
        assertEquals(0xABCD, ZipShort.getValue(buf, 1))
    }

    @Test
    fun equality() {
        assertEquals(ZipShort(42), ZipShort(42))
        assertEquals(ZipShort.ZERO, ZipShort(0))
    }
}

class ZipLongTest {

    @Test
    fun toBytes() {
        val zl = ZipLong(0x12345678L)
        assertContentEquals(byteArrayOf(0x78, 0x56, 0x34, 0x12), zl.bytes)
    }

    @Test
    fun fromBytes() {
        assertEquals(0x12345678L, ZipLong(byteArrayOf(0x78, 0x56, 0x34, 0x12)).value)
        // Values above Int.MAX_VALUE stay positive.
        assertEquals(0xFFFFFFFFL, ZipLong(byteArrayOf(-1, -1, -1, -1)).value)
    }

    @Test
    fun signatures() {
        assertContentEquals(byteArrayOf(0x50, 0x4B, 0x01, 0x02), ZipLong.CFH_SIG.bytes)
        assertContentEquals(byteArrayOf(0x50, 0x4B, 0x03, 0x04), ZipLong.LFH_SIG.bytes)
        assertContentEquals(byteArrayOf(0x50, 0x4B, 0x07, 0x08), ZipLong.DD_SIG.bytes)
    }
}

class ZipEightByteIntegerTest {

    @Test
    fun roundTrip() {
        val values = listOf(0L, 1L, 0xFFFFFFFFL, Long.MAX_VALUE, -1L)
        for (value in values) {
            assertEquals(
                value,
                ZipEightByteInteger(ZipEightByteInteger.getBytes(value)).longValue
            )
        }
    }

    @Test
    fun fromBytes() {
        val bytes = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(1L, ZipEightByteInteger.getLongValue(bytes))
        // All bits set: -1 in two's complement, 2^64 - 1 unsigned.
        assertEquals(-1L, ZipEightByteInteger(ByteArray(8) { -1 }).longValue)
    }
}

class GeneralPurposeBitTest {

    @Test
    fun parseUtf8Flag() {
        val data = ZipShort.getBytes(1 shl 11)
        val b = GeneralPurposeBit.parse(data, 0)
        assertTrue(b.usesUTF8ForNames())
        assertFalse(b.usesDataDescriptor())
        assertFalse(b.usesEncryption())
    }

    @Test
    fun parseDataDescriptorFlag() {
        val data = ZipShort.getBytes(1 shl 3)
        val b = GeneralPurposeBit.parse(data, 0)
        assertTrue(b.usesDataDescriptor())
        assertFalse(b.usesUTF8ForNames())
    }

    @Test
    fun encodeRoundTrip() {
        val b = GeneralPurposeBit()
        b.useUTF8ForNames(true)
        b.useDataDescriptor(true)
        assertEquals(b, GeneralPurposeBit.parse(b.encode(), 0))
    }
}

class ZipMethodTest {

    @Test
    fun methodByCode() {
        assertEquals(ZipMethod.STORED, ZipMethod.getMethodByCode(0))
        assertEquals(ZipMethod.DEFLATED, ZipMethod.getMethodByCode(8))
        assertEquals(null, ZipMethod.getMethodByCode(42))
    }
}
