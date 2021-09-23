/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.archive

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@RunWith(Parameterized::class)
class ArchiveTest(val archive: Archive) {

    companion object {

        @Parameterized.Parameters
        @JvmStatic
        fun archives(): List<Archive> {
            val epubZip = ArchiveTest::class.java.getResource("epub.epub")
            assertNotNull(epubZip)
            val zipArchive = runBlocking { DefaultArchiveFactory().open(File(epubZip.path), password = null) }
            assertNotNull(zipArchive)

            val epubExploded = ArchiveTest::class.java.getResource("epub")
            assertNotNull(epubExploded)
            val explodedArchive = runBlocking { DefaultArchiveFactory().open(File(epubExploded.path), password = null) }
            assertNotNull(explodedArchive)

            return listOf(zipArchive, explodedArchive)
        }
    }

    @Test
    fun `Entry list is correct`() {
        assertThat(runBlocking { archive.entries().map { it.path } })
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

    @Test
    fun `Attempting to get a missing entry throws`() {
        assertFails { runBlocking { archive.entry("unknown") } }
    }

    @Test
    fun `Fully reading an entry works well`() {
        val bytes = runBlocking { archive.entry("mimetype").read() }
        assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a range of an entry works well`() {
        val bytes = runBlocking { archive.entry("mimetype").read(0..10L) }
        assertEquals("application", bytes.toString(StandardCharsets.UTF_8))
        assertEquals(11, bytes.size)
    }

    @Test
    fun `Out of range indexes are clamped to the available length`() {
        val bytes = runBlocking { archive.entry("mimetype").read(-5..60L) }
        assertEquals("application/epub+zip", bytes.toString(StandardCharsets.UTF_8))
        assertEquals(20, bytes.size)
    }

    @Test
    fun `Decreasing ranges are understood as empty ones`() {
        val bytes = runBlocking { archive.entry("mimetype").read(60..20L) }
        assertEquals("", bytes.toString(StandardCharsets.UTF_8))
        assertEquals(0, bytes.size)
    }

    @Test
    fun `Computing size works well`() {
        val size = runBlocking { archive.entry("mimetype").length }
        assertEquals(20L, size)
    }
}