/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.fetcher

import android.webkit.MimeTypeMap
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.lengthBlocking
import org.readium.r2.shared.linkBlocking
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.readBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
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

        fun createLink(href: String, type: String?, entryLength: Long, isCompressed: Boolean): Link {
            val props = mutableMapOf<String, Any>(
                "archive" to mapOf(
                    "entryLength" to entryLength,
                    "isEntryCompressed" to isCompressed
                )
            )
            if (isCompressed) {
                props["compressedLength"] = entryLength
            }

            return Link(
                href = href,
                type = type,
                properties = Properties(props)
            )
        }

        assertEquals(
            listOf(
                createLink("/mimetype", null, 20, false),
                createLink("/EPUB/cover.xhtml", "application/xhtml+xml", 259L, true),
                createLink("/EPUB/css/epub.css", "text/css", 595L, true),
                createLink("/EPUB/css/nav.css", "text/css", 306L, true),
                createLink("/EPUB/images/cover.png", "image/png", 35809L, true),
                createLink("/EPUB/nav.xhtml", "application/xhtml+xml", 2293L, true),
                createLink("/EPUB/package.opf", null, 773L, true),
                createLink("/EPUB/s04.xhtml", "application/xhtml+xml", 118269L, true),
                createLink("/EPUB/toc.ncx", null, 1697, true),
                createLink("/META-INF/container.xml", "text/xml", 176, true)
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
        assertFailsWith<Resource.Exception.NotFound> { resource.readBlocking().getOrThrow() }
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
    fun `Adds compressed length and archive properties to the Link`() = runBlocking {
        assertJSONEquals(
            JSONObject(
                mapOf(
                    "compressedLength" to 595L,
                    "archive" to mapOf(
                        "entryLength" to 595L,
                        "isEntryCompressed" to true
                    )
                )
            ),
            fetcher.get(Link(href = "/EPUB/css/epub.css")).link().properties.toJSON()
        )
    }

    @Test
    fun `Original link properties are kept`() {
        val resource = fetcher.get(Link(href = "/mimetype", properties = Properties(mapOf("other" to "property"))))

        assertEquals(
            Link(
                href = "/mimetype",
                properties = Properties(
                    mapOf(
                        "other" to "property",
                        "archive" to mapOf("entryLength" to 20L, "isEntryCompressed" to false)
                    )
                )
            ),
            resource.linkBlocking()
        )
    }

    /**
     * When the HREF contains query parameters, the fetcher should first be able to remove them as
     * a fallback.
     */
    @Test
    fun `Get resource from HREF with query parameters`() = runBlocking {
        val resource = fetcher.get(Link(href = "/mimetype?query=param"))
        val result = resource.readAsString().getOrNull()
        assertEquals("application/epub+zip", result)
    }

    /**
     * When the HREF contains an anchor, the fetcher should first be able to remove them as
     * a fallback.
     */
    @Test
    fun `Get resource from HREF with anchors`() = runBlocking {
        val resource = fetcher.get(Link(href = "/mimetype#anchor"))
        val result = resource.readAsString().getOrNull()
        assertEquals("application/epub+zip", result)
    }
}
