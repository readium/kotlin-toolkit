/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.utils

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/** A [ZipInputStream] over an in-memory array, serving at most [chunkSize] bytes per read. */
internal class ByteArrayZipInputStream(
    private val data: ByteArray,
    private val chunkSize: Int = Int.MAX_VALUE,
) : ZipInputStream() {

    private var position = 0

    override suspend fun read(b: ByteArray, off: Int, len: Int): Int {
        if (position >= data.size) {
            return -1
        }
        val toRead = minOf(len, chunkSize, data.size - position)
        data.copyInto(b, off, position, position + toRead)
        position += toRead
        return toRead
    }
}

class ZipInputStreamTest {

    private val data = ByteArray(100) { it.toByte() }

    @Test
    fun singleByteReads() = runTest {
        val stream = ByteArrayZipInputStream(byteArrayOf(0x42, -1))
        assertEquals(0x42, stream.read())
        assertEquals(0xFF, stream.read())
        assertEquals(-1, stream.read())
    }

    @Test
    fun skips() = runTest {
        val stream = ByteArrayZipInputStream(data, chunkSize = 7)
        assertEquals(10, stream.skip(10))
        assertEquals(10, stream.read())
        assertEquals(89, stream.skip(200))
        assertEquals(-1, stream.read())
    }

    @Test
    fun countingStreamCountsBytes() = runTest {
        val counting = CountingInputStream(ByteArrayZipInputStream(data, chunkSize = 7))
        assertEquals(0, counting.bytesRead)
        counting.read()
        assertEquals(1, counting.bytesRead)
        val buffer = ByteArray(50)
        var read = 0
        while (read < 50) {
            read += counting.read(buffer, read, 50 - read)
        }
        assertEquals(51, counting.bytesRead)
        assertContentEquals(data.copyOfRange(1, 51), buffer)
    }

    @Test
    fun bufferedStreamPreservesContent() = runTest {
        val buffered = BufferedZipInputStream(ByteArrayZipInputStream(data, chunkSize = 3), 8)
        assertEquals(IOUtils.toByteArray(buffered).toList(), data.toList())
    }

    @Test
    fun bufferedStreamSingleByteReads() = runTest {
        val buffered = BufferedZipInputStream(ByteArrayZipInputStream(data), 4)
        for (i in 0 until 100) {
            assertEquals(i, buffered.read())
        }
        assertEquals(-1, buffered.read())
    }
}
