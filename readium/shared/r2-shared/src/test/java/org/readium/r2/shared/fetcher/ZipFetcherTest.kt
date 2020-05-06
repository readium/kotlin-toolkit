/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.junit.Test
import org.readium.r2.shared.publication.Link
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ZipFetcherTest {

    private val fetcher: Fetcher

    init {
        val epub = ZipFetcherTest::class.java.getResource("epub.epub")
        assertNotNull(epub)
        val zipFetcher = ZipFetcher.fromPath(epub.path)
        assertNotNull(zipFetcher)
        fetcher = zipFetcher
    }

    @Test
    fun `Reading a missing entry returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        val result = resource.length
        assert(result.isFailure)
        assertEquals(Resource.Error.NotFound, result.failure)
    }

    @Test
    fun `Computing length for a missing entry returns NotFound`() {
        val resource = fetcher.get(Link(href = "/unknown"))
        val result = resource.read()
        assert(result.isFailure)
        assertEquals(Resource.Error.NotFound, result.failure)
    }

    @Test
    fun `Fully reading an entry works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.read()
        assert(result.isSuccess)
        assertEquals("application/epub+zip", result.success.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a range of an entry works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.read(0..10L)
        assert(result.isSuccess)
        assertEquals("application", result.success.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Computing length works well`() {
        val resource = fetcher.get(Link(href = "/mimetype"))
        val result = resource.length
        assert(result.isSuccess)
        assertEquals(20L, result.success)

    }

}