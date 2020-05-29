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
        fetcher = FileFetcher(mapOf("/file_href" to text.path, "/dir_href" to directory.path))
    }

    @Test
    fun `Computing length for a missing file returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        val result = resource.length
        assert(result.isFailure)
        assertEquals(Resource.Error.NotFound, result.failure)
    }

    @Test
    fun `Reading a missing file returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        val result = resource.read()
        assert(result.isFailure)
        assertEquals(Resource.Error.NotFound, result.failure)
    }

    @Test
    fun `Reading an href in the map works well`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.read()
        assert(result.isSuccess)
        assertEquals("text", result.success.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a file in a directory works well`(){
        val resource = fetcher.get(Link(href = "/dir_href/text1.txt"))
        val result = resource.read()
        assert(result.isSuccess)
        assertEquals("text1", result.success.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a file in a subdirectory works well`(){
        val resource = fetcher.get(Link(href = "/dir_href/subdirectory/text2.txt"))
        val result = resource.read()
        assert(result.isSuccess)
        assertEquals("text2", result.success.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a directory returns NotFound`() {
        val resource = fetcher.get(Link(href = "/dir_href/subdirectory"))
        val result = resource.read()
        assert(result.isFailure)
        assertEquals(Resource.Error.NotFound, result.failure)
    }
    
    @Test
    fun `Reading a file outside the allowed directory returns NotFound`() {
        val resource = fetcher.get(Link(href = "/dir_href/../text.txt"))
        val result = resource.read()
        assert(result.isFailure)
        assertEquals(Resource.Error.NotFound, result.failure)
    }

    @Test
    fun `Reading a range works well`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.read(0..2L)
        assert(result.isSuccess)
        assertEquals("tex", result.success.toString(StandardCharsets.UTF_8))
    }


    @Test
    fun `Out of range indexes are clamped to the available length`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.read(-5..60L)
        assert(result.isSuccess)
        assertEquals("text", result.success.toString(StandardCharsets.UTF_8))
        assertEquals(4, result.success.size)
    }

    @Test
    @Suppress("EmptyRange")
    fun `Decreasing ranges are understood as empty ones`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.read(60..20L)
        assert(result.isSuccess)
        assertEquals("", result.success.toString(StandardCharsets.UTF_8))
        assertEquals(0, result.success.size)
    }

    @Test
    fun `Computing length works well`() {
        val resource = fetcher.get(Link(href = "/file_href"))
        val result = resource.length
        assert(result.isSuccess)
        assertEquals(4L, result.success)
    }

    @Test
    fun `Computing links works well`() {
        Shadows.shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypMapping("txt", "text/plain")
            addExtensionMimeTypMapping("mp3", "audio/mpeg")
        }

        assertEquals(
            listOf(
                Link(href = "/dir_href/subdirectory/hello.mp3", type = "audio/mpeg"),
                Link(href = "/dir_href/subdirectory/text2.txt", type = "text/plain"),
                Link(href = "/dir_href/text1.txt", type = "text/plain"),
                Link(href = "/file_href", type = "text/plain")
            ),
            fetcher.links
        )
    }

}
