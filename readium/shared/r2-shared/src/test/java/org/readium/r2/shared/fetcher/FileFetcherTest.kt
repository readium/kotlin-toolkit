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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.lengthBlocking
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.readBlocking
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class FileFetcherTest {

    private val fetcher: Fetcher

    init {
        val text = FileFetcherTest::class.java.getResource("text.txt")
        assertNotNull(text)
        val directory = FileFetcherTest::class.java.getResource("directory")
        assertNotNull(directory)
        fetcher = FileFetcher(mapOf("/file_href" to File(text.path), "/dir_href" to File(directory.path)))
    }

    @Test
    fun `Computing length for a missing file returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        assertFailsWith<Resource.Exception.NotFound> { resource.lengthBlocking().getOrThrow() }
    }

    @Test
    fun `Reading a missing file returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        assertFailsWith<Resource.Exception.NotFound> { resource.readBlocking().getOrThrow() }
    }

    @Test
    fun `Reading an href in the map works well`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.readBlocking().getOrNull()
        assertEquals("text", result?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a file in a directory works well`(){
        val resource = fetcher.get(Link(href = "/dir_href/text1.txt"))
        val result = resource.readBlocking().getOrNull()
        assertEquals("text1", result?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a file in a subdirectory works well`(){
        val resource = fetcher.get(Link(href = "/dir_href/subdirectory/text2.txt"))
        val result = resource.readBlocking().getOrNull()
        assertEquals("text2", result?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a directory returns NotFound`() {
        val resource = fetcher.get(Link(href = "/dir_href/subdirectory"))
        assertFailsWith<Resource.Exception.NotFound> { resource.readBlocking().getOrThrow() }
    }
    
    @Test
    fun `Reading a file outside the allowed directory returns NotFound`() {
        val resource = fetcher.get(Link(href = "/dir_href/../text.txt"))
        assertFailsWith<Resource.Exception.NotFound> { resource.readBlocking().getOrThrow() }
    }

    @Test
    fun `Reading a range works well`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.readBlocking(0..2L).getOrNull()
        assertEquals("tex", result?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading two ranges with the same resource work well`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result1 = resource.readBlocking(0..1L).getOrNull()
        assertEquals("te", result1?.toString(StandardCharsets.UTF_8))
        val result2 = resource.readBlocking(1..3L).getOrNull()
        assertEquals("ext", result2?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Out of range indexes are clamped to the available length`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.readBlocking(-5..60L).getOrNull()
        assertEquals("text", result?.toString(StandardCharsets.UTF_8))
        assertEquals(4, result?.size)
    }

    @Test
    @Suppress("EmptyRange")
    fun `Decreasing ranges are understood as empty ones`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.readBlocking(60..20L).getOrNull()
        assertEquals("", result?.toString(StandardCharsets.UTF_8))
        assertEquals(0, result?.size)
    }

    @Test
    fun `Computing length works well`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.lengthBlocking().getOrNull()
        assertEquals(4L, result)
    }

    @Test
    fun `Computing a directory length returns NotFound`() {
        val resource = fetcher.get(Link(href = "/dir_href/subdirectory"))
        assertFailsWith<Resource.Exception.NotFound> { resource.lengthBlocking().getOrThrow() }
    }

    @Test
    fun `Computing the length of a missing file returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        assertFailsWith<Resource.Exception.NotFound> { resource.lengthBlocking().getOrThrow() }
    }

    @Test
    fun `Computing links works well`() {
        Shadows.shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypMapping("txt", "text/plain")
            addExtensionMimeTypMapping("mp3", "audio/mpeg")
        }

        assertThat(runBlocking { fetcher.links() }).containsExactlyInAnyOrder(
            Link(href = "/dir_href/subdirectory/hello.mp3", type = "audio/mpeg"),
            Link(href = "/dir_href/subdirectory/text2.txt", type = "text/plain"),
            Link(href = "/dir_href/text1.txt", type = "text/plain"),
            Link(href = "/file_href", type = "text/plain")
        )
    }

}
