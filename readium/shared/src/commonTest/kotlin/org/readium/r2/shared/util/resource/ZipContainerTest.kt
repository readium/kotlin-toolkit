/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.resource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.DirectoryContainer
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.fromFilePath
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.zip.FileZipArchiveProvider
import org.readium.r2.shared.util.zip.StreamingZipArchiveProvider

class ZipContainerTest {

    private val epubZip: AbsoluteUrl = assertNotNull(
        AbsoluteUrl.fromFilePath(Fixtures("resource").path("epub.epub").toString())
    )

    private val epubExploded: AbsoluteUrl = assertNotNull(
        AbsoluteUrl.fromFilePath(
            Fixtures("resource").path("epub").toString(),
            isDirectory = true
        )
    )

    private val format = Format(
        specification = FormatSpecification(Specification.Zip, Specification.Epub),
        mediaType = MediaType.EPUB,
        fileExtension = FileExtension("epub")
    )

    private val archives: List<suspend () -> Container<Resource>> = listOf(
        // File-based zip container.
        suspend {
            assertNotNull(
                FileZipArchiveProvider()
                    .open(format, epubZip)
                    .getOrNull()
            )
        },
        // Streaming zip container.
        suspend {
            StreamingZipArchiveProvider()
                .openFile(epubZip)
        },
        // Exploded archive, as a behavioral reference.
        suspend {
            assertNotNull(
                DirectoryContainer(epubExploded).checkSuccess()
            )
        }
    )

    private fun eachSut(block: suspend (Container<Resource>) -> Unit) = runTest {
        for (archive in archives) {
            archive().use { container ->
                block(container)
            }
        }
    }

    @Test
    fun entryListIsCorrect() = eachSut { container ->
        val expected = setOf(
            Url("mimetype")!!,
            Url("EPUB/cover.xhtml")!!,
            Url("EPUB/css/epub.css")!!,
            Url("EPUB/css/nav.css")!!,
            Url("EPUB/images/cover.png")!!,
            Url("EPUB/nav.xhtml")!!,
            Url("EPUB/package.opf")!!,
            Url("EPUB/s04.xhtml")!!,
            Url("EPUB/toc.ncx")!!,
            Url("META-INF/container.xml")!!
        )
        assertTrue(
            container.entries.containsAll(expected),
            "Expected entries $expected in ${container.entries}"
        )
    }

    @Test
    fun attemptingToReadAMissingEntryReturnsNull() = eachSut { container ->
        assertNull(container[Url("unknown")!!])
    }

    @Test
    fun fullyReadingAnEntryWorksWell() = eachSut { container ->
        val resource = assertNotNull(container[Url("mimetype")!!])
        val bytes = resource.read().checkSuccess()
        assertEquals("application/epub+zip", bytes.decodeToString())
    }

    @Test
    fun readingARangeOfAnEntryWorksWell() = eachSut { container ->
        val resource = assertNotNull(container[Url("mimetype")!!])
        val bytes = resource.read(0..10L).checkSuccess()
        assertEquals("application", bytes.decodeToString())
        assertEquals(11, bytes.size)
    }

    @Test
    fun outOfRangeIndexesAreClampedToTheAvailableLength() = eachSut { container ->
        val resource = assertNotNull(container[Url("mimetype")!!])
        val bytes = resource.read(-5..60L).checkSuccess()
        assertEquals("application/epub+zip", bytes.decodeToString())
        assertEquals(20, bytes.size)
    }

    @Suppress("EmptyRange")
    @Test
    fun decreasingRangesAreUnderstoodAsEmptyOnes() = eachSut { container ->
        val resource = assertNotNull(container[Url("mimetype")!!])
        val bytes = resource.read(60..20L).checkSuccess()
        assertEquals("", bytes.decodeToString())
        assertEquals(0, bytes.size)
    }

    @Test
    fun computingSizeWorksWell() = eachSut { container ->
        val resource = assertNotNull(container[Url("mimetype")!!])
        val size = resource.length().checkSuccess()
        assertEquals(20L, size)
    }
}
