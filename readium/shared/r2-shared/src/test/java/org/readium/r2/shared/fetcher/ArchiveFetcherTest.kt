/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import android.webkit.MimeTypeMap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class ArchiveFetcherTest {

    private val fetcher: Fetcher

    init {
        val epub = ArchiveFetcherTest::class.java.getResource("epub.epub")
        assertNotNull(epub)
        val zipFetcher = ArchiveFetcher.fromPath(epub.path)
        assertNotNull(zipFetcher)
        fetcher = zipFetcher
    }

    @Test
    fun `Link list is correct`() {
        Shadows.shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypMapping("css", "text/css")
            addExtensionMimeTypMapping("png", "image/png")
            addExtensionMimeTypMapping("xml", "text/xml")
        }

        assertEquals(
            listOf(
                "/mimetype" to null,
                "/EPUB/cover.xhtml" to "text/html",
                "/EPUB/css/epub.css" to "text/css",
                "/EPUB/css/nav.css" to "text/css",
                "/EPUB/images/cover.png" to "image/png",
                "/EPUB/nav.xhtml" to "text/html",
                "/EPUB/package.opf" to null,
                "/EPUB/s04.xhtml" to "text/html",
                "/EPUB/toc.ncx" to null,
                "/META-INF/container.xml" to "text/xml"
            ).map { (href, type) -> Link(href = href, type = type) }.toList(),
            fetcher.links
        )
    }

    @Test
    fun `Computing length for a missing entry returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        assertFailsWith<Resource.Error.NotFound> { resource.length.getOrThrow() }
    }

    @Test
    fun `Reading a missing entry returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        assertFailsWith<Resource.Error.NotFound> { resource.length.getOrThrow() }
    }

    @Test
    fun `Fully reading an entry works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.read().getOrNull()
        assertEquals("application/epub+zip", result?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a range of an entry works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.read(0..10L).getOrNull()
        assertEquals("application", result?.toString(StandardCharsets.UTF_8))
        assertEquals(11, result?.size)
    }

    @Test
    fun `Out of range indexes are clamped to the available length`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.read(-5..60L).getOrNull()
        assertEquals("application/epub+zip", result?.toString(StandardCharsets.UTF_8))
        assertEquals(20, result?.size)
    }

    @Test
    fun `Decreasing ranges are understood as empty ones`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.read(60..20L).getOrNull()
        assertEquals("", result?.toString(StandardCharsets.UTF_8))
        assertEquals(0, result?.size)
    }

    @Test
    fun `Computing length works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.length
        assertEquals(20L, result.getOrNull())
    }
}
