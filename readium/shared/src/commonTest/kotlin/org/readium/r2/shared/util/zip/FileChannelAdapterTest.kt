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
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.fixturesFileSystem
import org.readium.r2.shared.util.zip.jvm.ClosedChannelException
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

class FileChannelAdapterTest {

    // "Hello, Readium!\n"
    private val fixtures = Fixtures("kmp")
    private val content = fixtures.read("hello.txt").toByteArray()

    private fun open(): FileChannelAdapter =
        FileChannelAdapter(fixturesFileSystem.openReadOnly(fixtures.path("hello.txt")))

    @Test
    fun sequentialReadsAdvancePosition() = runTest {
        val channel = open()
        try {
            val buffer = ZipBuffer.allocate(5)
            assertEquals(5, channel.read(buffer))
            assertContentEquals(content.copyOfRange(0, 5), buffer.array())
            assertEquals(5, channel.position())

            buffer.clear()
            assertEquals(5, channel.read(buffer))
            assertContentEquals(content.copyOfRange(5, 10), buffer.array())
            assertEquals(10, channel.position())
        } finally {
            channel.close()
        }
    }

    @Test
    fun randomAccessReads() = runTest {
        val channel = open()
        try {
            channel.position(7)
            val buffer = ZipBuffer.allocate(4)
            assertEquals(4, channel.read(buffer))
            assertContentEquals(content.copyOfRange(7, 11), buffer.array())
        } finally {
            channel.close()
        }
    }

    @Test
    fun readIsClampedAtEndOfFileAndThenReturnsMinusOne() = runTest {
        val channel = open()
        try {
            val size = content.size.toLong()
            channel.position(size - 2)
            val buffer = ZipBuffer.allocate(8)
            assertEquals(2, channel.read(buffer))
            assertEquals(-1, channel.read(ZipBuffer.allocate(8)))
        } finally {
            channel.close()
        }
    }

    @Test
    fun sizeReturnsFileLength() = runTest {
        val channel = open()
        try {
            assertEquals(content.size.toLong(), channel.size())
        } finally {
            channel.close()
        }
    }

    @Test
    fun writeAndTruncateAreUnsupported() = runTest {
        val channel = open()
        try {
            assertFailsWith<NonWritableChannelException> { channel.write(ZipBuffer.allocate(1)) }
            assertFailsWith<NonWritableChannelException> { channel.truncate(2) }
        } finally {
            channel.close()
        }
    }

    @Test
    fun closeForbidsFurtherOperations() = runTest {
        val channel = open()
        assertTrue(channel.isOpen)
        channel.close()
        assertFalse(channel.isOpen)
        assertFailsWith<ClosedChannelException> { channel.read(ZipBuffer.allocate(1)) }
        assertFailsWith<ClosedChannelException> { channel.size() }
    }
}
