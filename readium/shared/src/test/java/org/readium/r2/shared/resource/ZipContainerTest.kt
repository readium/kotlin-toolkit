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
import org.junit.runners.Parameterized
import org.readium.r2.shared.error.getOrThrow
import org.readium.r2.shared.util.archive.channel.ChannelZipArchiveFactory

@RunWith(Parameterized::class)
class ZipContainerTest(val container: Container) {

    companion object {

        @Parameterized.Parameters
        @JvmStatic
        fun archives(): List<Container> {
            val epubZip = ZipContainerTest::class.java.getResource("epub.epub")
            assertNotNull(epubZip)
            val zipArchive = runBlocking {
                DefaultArchiveFactory()
                    .create(FileResource(File(epubZip.path)), password = null)
                    .successOrNull()
            }
            assertNotNull(zipArchive)
            val apacheZipArchive = runBlocking {
                ChannelZipArchiveFactory()
                    .openFile(File(epubZip.path))
            }
            assertNotNull(apacheZipArchive)

            val epubExploded = ZipContainerTest::class.java.getResource("epub")
            assertNotNull(epubExploded)
            val explodedArchive = runBlocking {
                DirectoryContainerFactory()
                    .create(File(epubExploded.path))
                    .successOrNull()
            }
            assertNotNull(explodedArchive)

            return listOf(zipArchive, apacheZipArchive, explodedArchive)
        }
    }

    @Test
    fun `Entry list is correct`() {
        assertThat(runBlocking { container.entries()?.map { it.path }.orEmpty() })
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

    @Test
    fun `Attempting to read a missing entry throws`() {
        assertFails { runBlocking { container.entry("unknown").read().getOrThrow() } }
    }

    @Test
    fun `Fully reading an entry works well`() {
        val bytes = runBlocking { container.entry("mimetype").read().getOrThrow() }
        assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a range of an entry works well`() {
        val bytes = runBlocking { container.entry("mimetype").read(0..10L).getOrThrow() }
        assertEquals("application", bytes.toString(StandardCharsets.UTF_8))
        assertEquals(11, bytes.size)
    }

    @Test
    fun `Out of range indexes are clamped to the available length`() {
        val bytes = runBlocking { container.entry("mimetype").read(-5..60L).getOrThrow() }
        assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
        assertEquals(20, bytes.size)
    }

    @Test
    fun `Decreasing ranges are understood as empty ones`() {
        val bytes = runBlocking { container.entry("mimetype").read(60..20L).getOrThrow() }
        assertEquals("", bytes.toString(StandardCharsets.UTF_8))
        assertEquals(0, bytes.size)
    }

    @Test
    fun `Computing size works well`() {
        val size = runBlocking { container.entry("mimetype").length().getOrThrow() }
        assertEquals(20L, size)
    }
}
