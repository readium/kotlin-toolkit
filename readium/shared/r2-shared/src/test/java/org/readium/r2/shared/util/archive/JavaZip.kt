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
import org.junit.Test
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JavaZipTest {

    private val archive: Archive

    init {
        val epub = JavaZipTest::class.java.getResource("epub.epub")
        assertNotNull(epub)
        val zipArchive = runBlocking { JavaZip.open(epub.path) }
        assertNotNull(zipArchive)
        archive = zipArchive
    }

    @Test
    fun `Entry list is correct`() {
        assertEquals(
            listOf(
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
            ),
            runBlocking { archive.entries().map { it.path } }
        )
    }

    @Test
    fun `size returns null for a missing entry `() {
        assertNull(runBlocking { archive.entry("unknown") })
    }

    @Test
    fun `compressedSize returns null for a missing entry `() {
        assertNull(runBlocking { archive.entry("unknown") })
    }

    @Test
    fun `Fully reading an entry works well`() {
        val bytes = runBlocking { archive.entry("mimetype")?.read() }
        assertEquals("application/epub+zip", bytes?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a range of an entry works well`() {
        val bytes = runBlocking { archive.entry("mimetype")?.read(0..10L) }
        assertEquals("application", bytes?.toString(StandardCharsets.UTF_8))
        assertEquals(11, bytes?.size)
    }

    @Test
    fun `Out of range indexes are clamped to the available length`() {
        val bytes = runBlocking { archive.entry("mimetype")?.read(-5..60L) }
        assertEquals("application/epub+zip", bytes?.toString(StandardCharsets.UTF_8))
        assertEquals(20, bytes?.size)
    }

    @Test
    fun `Decreasing ranges are understood as empty ones`() {
        val bytes = runBlocking { archive.entry("mimetype")?.read(60..20L) }
        assertEquals("", bytes?.toString(StandardCharsets.UTF_8))
        assertEquals(0, bytes?.size)
    }

    @Test
    fun `Computing size works well`() {
        val size = runBlocking { archive.entry("mimetype")?.size }
        assertEquals(20L, size)
    }
}