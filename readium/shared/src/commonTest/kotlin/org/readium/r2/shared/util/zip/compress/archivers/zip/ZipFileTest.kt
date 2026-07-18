/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.fixturesFileSystem
import org.readium.r2.shared.util.zip.FileChannelAdapter
import org.readium.r2.shared.util.zip.compress.utils.IOUtils
import org.readium.r2.shared.util.zip.compress.utils.InputStreamStatistics
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel

class ZipFileTest {

    private val fixtures = Fixtures("zip")

    private fun openChannel(name: String): SeekableByteChannel =
        FileChannelAdapter(fixturesFileSystem.openReadOnly(fixtures.path(name)))

    private suspend fun openZip(name: String): ZipFile =
        ZipFile(openChannel(name))

    private suspend fun ZipFile.readEntryFully(entry: ZipArchiveEntry): ByteArray {
        val stream = assertNotNull(getInputStream(entry))
        return try {
            IOUtils.toByteArray(stream)
        } finally {
            stream.close()
        }
    }

    @Test
    fun readsStoredAndDeflatedEntries() = runTest {
        val zip = openZip("basic.zip")
        try {
            val stored = assertNotNull(zip.getEntry("stored.txt"))
            assertEquals(ZipArchiveEntry.STORED, stored.method)
            val storedExpected = fixtures.read("basic-stored.bin").toByteArray()
            assertEquals(storedExpected.size.toLong(), stored.size)
            assertContentEquals(storedExpected, zip.readEntryFully(stored))

            val deflated = assertNotNull(zip.getEntry("dir/deflated.txt"))
            assertEquals(ZipArchiveEntry.DEFLATED, deflated.method)
            val deflatedExpected = fixtures.read("basic-deflated.bin").toByteArray()
            assertEquals(deflatedExpected.size.toLong(), deflated.size)
            assertTrue(deflated.compressedSize < deflated.size)
            assertContentEquals(deflatedExpected, zip.readEntryFully(deflated))
        } finally {
            zip.close()
        }
    }

    @Test
    fun exposesInputStreamStatistics() = runTest {
        val zip = openZip("basic.zip")
        try {
            val deflated = assertNotNull(zip.getEntry("dir/deflated.txt"))
            val stream = assertNotNull(zip.getInputStream(deflated))
            try {
                IOUtils.toByteArray(stream)
                val statistics = assertIs<InputStreamStatistics>(stream)
                assertEquals(deflated.size, statistics.uncompressedCount)
                assertTrue(statistics.compressedCount >= deflated.compressedSize)
            } finally {
                stream.close()
            }
        } finally {
            zip.close()
        }
    }

    @Test
    fun rawInputStreamFromIndexOnStoredEntry() = runTest {
        val zip = openZip("basic.zip")
        try {
            val stored = assertNotNull(zip.getEntry("stored.txt"))
            val expected = fixtures.read("basic-stored.bin").toByteArray()
            val stream = assertNotNull(zip.getRawInputStream(stored, 10))
            try {
                assertContentEquals(
                    expected.copyOfRange(10, expected.size),
                    IOUtils.toByteArray(stream)
                )
            } finally {
                stream.close()
            }
        } finally {
            zip.close()
        }
    }

    @Test
    fun readsEpubFixture() = runTest {
        val zip = openZip("epub.epub")
        try {
            assertTrue(zip.entries.isNotEmpty())
            val mimetype = assertNotNull(zip.getEntry("mimetype"))
            assertEquals(
                "application/epub+zip",
                zip.readEntryFully(mimetype).decodeToString()
            )
            // Read a deflated entry and check its length matches the declared size.
            val css = assertNotNull(zip.getEntry("EPUB/css/epub.css"))
            assertEquals(css.size, zip.readEntryFully(css).size.toLong())
            // Unknown entries return null.
            assertNull(zip.getEntry("does/not/exist"))
        } finally {
            zip.close()
        }
    }

    @Test
    fun readsZip64Archive() = runTest {
        val zip = openZip("zip64.zip")
        try {
            assertEquals(1, zip.entries.size)
            val entry = assertNotNull(zip.getEntry("a.txt"))
            val expected = "hello zip64 world!\n".encodeToByteArray()
            assertEquals(expected.size.toLong(), entry.size)
            assertContentEquals(expected, zip.readEntryFully(entry))
        } finally {
            zip.close()
        }
    }

    @Test
    fun readsEntriesWithDataDescriptors() = runTest {
        val zip = openZip("datadescriptor.zip")
        try {
            val first = assertNotNull(zip.getEntry("first.txt"))
            assertTrue(first.generalPurposeBit.usesDataDescriptor())
            assertContentEquals(
                fixtures.read("basic-deflated.bin").toByteArray(),
                zip.readEntryFully(first)
            )
            val second = assertNotNull(zip.getEntry("second.txt"))
            assertContentEquals(
                fixtures.read("basic-stored.bin").toByteArray(),
                zip.readEntryFully(second)
            )
        } finally {
            zip.close()
        }
    }

    @Test
    fun readsUnicodeNames() = runTest {
        val zip = openZip("unicode.zip")
        try {
            // Name flagged as UTF-8 through the general purpose bit 11.
            val utf8Entry = assertNotNull(zip.getEntry("café/日本語.txt"))
            assertEquals(ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG, utf8Entry.nameSource)
            assertContentEquals(
                "utf-8 flagged name".encodeToByteArray(),
                zip.readEntryFully(utf8Entry)
            )

            // Name overridden by the InfoZIP Unicode path extra field.
            val extraFieldEntry = assertNotNull(zip.getEntry("café.txt"))
            assertEquals(
                ZipArchiveEntry.NameSource.UNICODE_EXTRA_FIELD,
                extraFieldEntry.nameSource
            )
            assertContentEquals(
                "unicode extra field name".encodeToByteArray(),
                zip.readEntryFully(extraFieldEntry)
            )
        } finally {
            zip.close()
        }
    }

    @Test
    fun entriesInPhysicalOrder() = runTest {
        val zip = openZip("basic.zip")
        try {
            val physical = zip.getEntriesInPhysicalOrder()
            assertEquals(zip.entries.map { it.name }, physical.map { it.name })
        } finally {
            zip.close()
        }
    }

    @Test
    fun failsOnGarbageInput() = runTest {
        val channel = openChannel("lorem.txt")
        try {
            kotlin.test.assertFailsWith<okio.IOException> {
                ZipFile(channel)
            }
        } finally {
            channel.close()
        }
    }
}
