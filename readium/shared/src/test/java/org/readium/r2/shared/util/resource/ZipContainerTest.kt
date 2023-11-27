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
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.assertSuccess
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.zip.FileZipArchiveProvider
import org.readium.r2.shared.util.zip.StreamingZipArchiveProvider
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class ZipContainerTest(val sut: suspend () -> Container) {

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun archives(): List<suspend () -> Container> {
            val epubZip = ZipContainerTest::class.java.getResource("epub.epub")
            assertNotNull(epubZip)

            val zipArchive = suspend {
                assertNotNull(
                    FileZipArchiveProvider(MediaTypeRetriever())
                        .create(
                            FileResource(File(epubZip.path), mediaType = MediaType.EPUB),
                            password = null
                        )
                        .getOrNull()
                )
            }

            val apacheZipArchive = suspend {
                StreamingZipArchiveProvider(MediaTypeRetriever())
                    .openFile(File(epubZip.path))
            }

            val epubExploded = ZipContainerTest::class.java.getResource("epub")
            assertNotNull(epubExploded)
            val explodedArchive = suspend {
                assertNotNull(
                    DirectoryContainerFactory(MediaTypeRetriever())
                        .create(File(epubExploded.path))
                        .getOrNull()
                )
            }
            assertNotNull(explodedArchive)

            return listOf(zipArchive, apacheZipArchive, explodedArchive)
        }
    }

    @Test
    fun `Entry list is correct`(): Unit = runBlocking {
        sut().use { container ->
            assertThat(container.entries()?.map { it.url.toString() })
                .contains(
                    "mimetype",
                    "EPUB/cover.xhtml",
                    "EPUB/css/epub.css",
                    "EPUB/css/nav.css",
                    "EPUB/images/cover.png",
                    "EPUB/nav.xhtml",
                    "EPUB/package.opf",
                    "EPUB/s04.xhtml",
                    "EPUB/toc.ncx",
                    "META-INF/container.xml"
                )
        }
    }

    @Test
    fun `Attempting to read a missing entry throws`(): Unit = runBlocking {
        sut().use { container ->
            assertFails { container.get(Url("unknown")!!).read().assertSuccess() }
        }
    }

    @Test
    fun `Fully reading an entry works well`(): Unit = runBlocking {
        sut().use { container ->
            val bytes = container.get(Url("mimetype")!!).read().assertSuccess()
            assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
        }
    }

    @Test
    fun `Reading a range of an entry works well`(): Unit = runBlocking {
        sut().use { container ->
            val bytes = container.get(Url("mimetype")!!).read(0..10L).assertSuccess()
            assertEquals("application", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(11, bytes.size)
        }
    }

    @Test
    fun `Out of range indexes are clamped to the available length`(): Unit = runBlocking {
        sut().use { container ->
            val bytes = container.get(Url("mimetype")!!).read(-5..60L).assertSuccess()
            assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(20, bytes.size)
        }
    }

    @Test
    fun `Decreasing ranges are understood as empty ones`(): Unit = runBlocking {
        sut().use { container ->
            val bytes = container.get(Url("mimetype")!!).read(60..20L).assertSuccess()
            assertEquals("", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(0, bytes.size)
        }
    }

    @Test
    fun `Computing size works well`(): Unit = runBlocking {
        sut().use { container ->
            val size = container.get(Url("mimetype")!!).length().assertSuccess()
            assertEquals(20L, size)
        }
    }
}
