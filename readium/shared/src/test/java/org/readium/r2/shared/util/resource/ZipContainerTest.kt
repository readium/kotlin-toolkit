/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.resource

import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.DirectoryContainer
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.zip.FileZipArchiveProvider
import org.readium.r2.shared.util.zip.StreamingZipArchiveProvider
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class ZipContainerTest(val sut: suspend () -> Container<Resource>) {

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun archives(): List<suspend () -> Container<Resource>> {
            val epubZip = ZipContainerTest::class.java.getResource("epub.epub")
            assertNotNull(epubZip)
            val format = Format(
                specification = FormatSpecification(Specification.Zip, Specification.Epub),
                mediaType = MediaType.EPUB,
                fileExtension = FileExtension("epub")
            )

            val zipArchive = suspend {
                assertNotNull(
                    FileZipArchiveProvider()
                        .open(format, File(epubZip.path))
                        .getOrNull()
                )
            }

            val apacheZipArchive = suspend {
                StreamingZipArchiveProvider()
                    .openFile(File(epubZip.path))
            }

            val epubExploded = ZipContainerTest::class.java.getResource("epub")
            assertNotNull(epubExploded)
            val explodedArchive = suspend {
                assertNotNull(
                    DirectoryContainer(File(epubExploded.path)).checkSuccess()
                )
            }
            assertNotNull(explodedArchive)

            return listOf(zipArchive, apacheZipArchive, explodedArchive)
        }
    }

    @Test
    fun `Entry list is correct`(): Unit = runBlocking {
        sut().use { container ->
            assertThat(container.entries)
                .contains(
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
        }
    }

    @Test
    fun `Attempting to read a missing entry throws`(): Unit = runBlocking {
        sut().use { container ->
            assertNull(container[Url("unknown")!!])
        }
    }

    @Test
    fun `Fully reading an entry works well`(): Unit = runBlocking {
        sut().use { container ->
            val resource = assertNotNull(container[Url("mimetype")!!])
            val bytes = resource.read().checkSuccess()
            assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
        }
    }

    @Test
    fun `Reading a range of an entry works well`(): Unit = runBlocking {
        sut().use { container ->
            val resource = assertNotNull(container[Url("mimetype")!!])
            val bytes = resource.read(0..10L).checkSuccess()
            assertEquals("application", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(11, bytes.size)
        }
    }

    @Test
    fun `Out of range indexes are clamped to the available length`(): Unit = runBlocking {
        sut().use { container ->
            val resource = assertNotNull(container[Url("mimetype")!!])
            val bytes = resource.read(-5..60L).checkSuccess()
            assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(20, bytes.size)
        }
    }

    @Suppress("EmptyRange")
    @Test
    fun `Decreasing ranges are understood as empty ones`(): Unit = runBlocking {
        sut().use { container ->
            val resource = assertNotNull(container[Url("mimetype")!!])
            val bytes = resource.read(60..20L).checkSuccess()
            assertEquals("", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(0, bytes.size)
        }
    }

    @Test
    fun `Computing size works well`(): Unit = runBlocking {
        sut().use { container ->
            val resource = assertNotNull(container[Url("mimetype")!!])
            val size = resource.length().checkSuccess()
            assertEquals(20L, size)
        }
    }
}
