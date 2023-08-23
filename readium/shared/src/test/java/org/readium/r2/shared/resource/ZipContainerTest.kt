/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.resource

import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.archive.channel.ChannelZipArchiveFactory
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.use
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
                    DefaultArchiveFactory(MediaTypeRetriever())
                        .create(
                            FileResource(File(epubZip.path), mediaType = MediaType.EPUB),
                            password = null
                        )
                        .getOrNull()
                )
            }

            val apacheZipArchive = suspend {
                ChannelZipArchiveFactory(MediaTypeRetriever())
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
            assertThat(container.entries()?.map { it.path })
                .contains(
                    "/mimetype",
                    "/EPUB/cover.xhtml",
                    "/EPUB/css/epub.css",
                    "/EPUB/css/nav.css",
                    "/EPUB/images/cover.png",
                    "/EPUB/nav.xhtml",
                    "/EPUB/package.opf",
                    "/EPUB/s04.xhtml",
                    "/EPUB/toc.ncx",
                    "/META-INF/container.xml"
                )
        }
    }

    @Test
    fun `Attempting to read a missing entry throws`(): Unit = runBlocking {
        sut().use { container ->
            assertFails { container.get("/unknown").read().getOrThrow() }
        }
    }

    @Test
    fun `Fully reading an entry works well`(): Unit = runBlocking {
        sut().use { container ->
            val bytes = container.get("/mimetype").read().getOrThrow()
            assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
        }
    }

    @Test
    fun `Reading a range of an entry works well`(): Unit = runBlocking {
        sut().use { container ->
            val bytes = container.get("/mimetype").read(0..10L).getOrThrow()
            assertEquals("application", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(11, bytes.size)
        }
    }

    @Test
    fun `Out of range indexes are clamped to the available length`(): Unit = runBlocking {
        sut().use { container ->
            val bytes = container.get("/mimetype").read(-5..60L).getOrThrow()
            assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(20, bytes.size)
        }
    }

    @Test
    fun `Decreasing ranges are understood as empty ones`(): Unit = runBlocking {
        sut().use { container ->
            val bytes = container.get("/mimetype").read(60..20L).getOrThrow()
            assertEquals("", bytes.toString(StandardCharsets.UTF_8))
            assertEquals(0, bytes.size)
        }
    }

    @Test
    fun `Computing size works well`(): Unit = runBlocking {
        sut().use { container ->
            val size = container.get("/mimetype").length().getOrThrow()
            assertEquals(20L, size)
        }
    }
}
