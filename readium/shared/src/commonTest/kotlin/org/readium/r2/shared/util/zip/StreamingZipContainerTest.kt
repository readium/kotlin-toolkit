/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use

/**
 * Exercises [StreamingZipContainer] against an in-memory [Readable] simulating ranged reads
 * (e.g. a zip served over HTTP), asserting that opening the container and reading entries
 * performs random access instead of downloading the whole archive (ADR 0002).
 */
class StreamingZipContainerTest {

    private val epubBytes: ByteArray =
        Fixtures("resource").read("epub.epub").toByteArray()

    /**
     * The tail size cached by StreamingZipArchiveProvider: maximum size of the ZIP "end of
     * central directory record" plus the Zip64 locator, mirroring CACHED_TAIL_SIZE.
     */
    private val cachedTailSize: Int = 65557

    /**
     * A large source: 8 MB of zero preamble followed by the EPUB fixture, so that the source
     * exceeds the 5 MB threshold under which the provider simply caches the whole archive.
     * ZIP readers support such self-extracting-style preambles.
     */
    private fun largeSource(): TrackingReadable {
        val preambleSize = 8 * 1024 * 1024
        val data = ByteArray(preambleSize + epubBytes.size)
        epubBytes.copyInto(data, preambleSize)
        return TrackingReadable(data)
    }

    private suspend fun open(readable: TrackingReadable): Container<Resource> =
        assertNotNull(
            StreamingZipArchiveProvider().sniffOpen(readable).getOrNull(),
            "the streaming provider should sniff the zip"
        )

    @Test
    fun opensAZipServedThroughRangedReads() = runTest {
        val readable = largeSource()
        open(readable).use { container ->
            assertTrue(Url("mimetype")!! in container.entries)
            assertTrue(Url("EPUB/cover.xhtml")!! in container.entries)
            assertEquals(10, container.entries.size)
        }
    }

    @Test
    fun openingReadsOnlyTheCachedTailWithASingleSourceRead() = runTest {
        val readable = largeSource()
        open(readable).use {
            val sourceSize = 8 * 1024 * 1024 + epubBytes.size
            assertEquals(
                listOf((sourceSize - cachedTailSize).toLong() until sourceSize.toLong()),
                readable.reads,
                "opening should perform exactly one ranged read, of the archive tail"
            )
        }
    }

    @Test
    fun readingAnEntryPerformsRandomAccessInsteadOfAFullDownload() = runTest {
        val readable = largeSource()
        open(readable).use { container ->
            val resource = assertNotNull(container[Url("mimetype")!!])
            val bytes = resource.read().checkSuccess()
            assertEquals("application/epub+zip", bytes.decodeToString())

            val totalBytesRead = readable.reads.sumOf { it.last - it.first + 1 }
            assertTrue(
                totalBytesRead <= cachedTailSize + 64 * 1024,
                "reading a single entry should not download the archive " +
                    "(read $totalBytesRead bytes)"
            )
            // Every read is bounded: either the cached tail, or a buffered chunk.
            for (read in readable.reads) {
                assertTrue(read.last - read.first + 1 <= cachedTailSize)
            }
        }
    }

    @Test
    fun readingAnEntryRangeDoesNotReadTheWholeEntry() = runTest {
        val readable = largeSource()
        open(readable).use { container ->
            val resource = assertNotNull(container[Url("EPUB/images/cover.png")!!])
            val length = resource.length().checkSuccess()
            val readsBefore = readable.reads.size

            val bytes = resource.read(0 until 1024L).checkSuccess()
            assertEquals(1024, bytes.size)

            val newReads = readable.reads.drop(readsBefore)
            val newBytesRead = newReads.sumOf { it.last - it.first + 1 }
            assertTrue(
                newBytesRead < length,
                "a ranged read should not fetch the whole entry " +
                    "($newBytesRead bytes read for a $length bytes entry)"
            )
        }
    }

    @Test
    fun smallSourcesAreCachedWithASingleSourceRead() = runTest {
        // Under the 5 MB threshold, the provider caches the whole archive upfront: exactly one
        // read of the full source, then no more accesses.
        val readable = TrackingReadable(epubBytes)
        open(readable).use { container ->
            val resource = assertNotNull(container[Url("mimetype")!!])
            assertEquals("application/epub+zip", resource.read().checkSuccess().decodeToString())
            assertEquals(
                listOf(0L until epubBytes.size.toLong()),
                readable.reads,
                "small sources should be read once, fully"
            )
        }
    }
}
