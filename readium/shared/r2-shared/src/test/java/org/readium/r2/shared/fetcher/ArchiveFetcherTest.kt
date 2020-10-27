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
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.lengthBlocking
import org.readium.r2.shared.linkBlocking
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.readBlocking
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
        val zipFetcher = runBlocking { ArchiveFetcher.fromPath(epub.path) }
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

        fun createLink(href: String, type: String?, compressedLength: Long? = null) = Link(
            href = href,
            type = type,
            properties =
                Properties(
                    compressedLength
                        ?.let {mapOf("compressedLength" to compressedLength) }
                        ?: mapOf()
                )
        )

        assertEquals(
            listOf(
                createLink("/mimetype", null),
                createLink("/EPUB/cover.xhtml" , "text/html", 259L),
                createLink("/EPUB/css/epub.css",  "text/css", 595L),
                createLink("/EPUB/css/nav.css", "text/css", 306L),
                createLink("/EPUB/images/cover.png", "image/png", 35809L),
                createLink("/EPUB/nav.xhtml", "text/html", 2293L),
                createLink("/EPUB/package.opf", null, 773L),
                createLink("/EPUB/s04.xhtml", "text/html", 118269L),
                createLink("/EPUB/toc.ncx", null, 1697),
                createLink("/META-INF/container.xml", "text/xml", 176)
            ),
            runBlocking { fetcher.links() }
        )
    }

    @Test
    fun `Computing length for a missing entry returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        assertFailsWith<Resource.Exception.NotFound> { resource.lengthBlocking().getOrThrow() }
    }

    @Test
    fun `Reading a missing entry returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        assertFailsWith<Resource.Exception.NotFound> { resource.lengthBlocking().getOrThrow() }
    }

    @Test
    fun `Fully reading an entry works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.readBlocking().getOrNull()
        assertEquals("application/epub+zip", result?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a range of an entry works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.readBlocking(0..10L).getOrNull()
        assertEquals("application", result?.toString(StandardCharsets.UTF_8))
        assertEquals(11, result?.size)
    }

    @Test
    fun `Out of range indexes are clamped to the available length`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.readBlocking(-5..60L).getOrNull()
        assertEquals("application/epub+zip", result?.toString(StandardCharsets.UTF_8))
        assertEquals(20, result?.size)
    }

    @Test
    fun `Decreasing ranges are understood as empty ones`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.readBlocking(60..20L).getOrNull()
        assertEquals("", result?.toString(StandardCharsets.UTF_8))
        assertEquals(0, result?.size)
    }

    @Test
    fun `Computing length works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.lengthBlocking()
        assertEquals(20L, result.getOrNull())
    }

    @Test
    fun `Computing a directory length returns NotFound`() {
        val resource = fetcher.get(Link(href = "/EPUB"))
        assertFailsWith<Resource.Exception.NotFound> { resource.lengthBlocking().getOrThrow() }
    }

    @Test
    fun `Computing the length of a missing file returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        assertFailsWith<Resource.Exception.NotFound> { resource.lengthBlocking().getOrThrow() }
    }


    @Test
    fun `Original link properties are kept`() {
        val resource = fetcher.get(Link(href = "/mimetype", properties = Properties(mapOf("other" to "property"))))

        assertEquals(
            Link(href = "/mimetype", properties = Properties(mapOf(
                "other" to "property"
            ))),
            resource.linkBlocking()
        )
    }

}
