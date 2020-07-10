/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.image

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.util.File
import org.readium.r2.shared.publication.firstWithRel
import org.readium.r2.streamer.extensions.fromFile
import org.readium.r2.streamer.parseBlocking
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImageParserTest {

    private val parser = ImageParser()

    private val cbzFile = fileForResource("futuristic_tales.cbz")
    private val cbzFetcher = fetcherForFile(cbzFile)

    private val jpgFile = fileForResource("futuristic_tales.jpg")
    private val jpgFetcher = fetcherForFile(jpgFile)

    private fun fileForResource(resource: String): File {
        val path = ImageParserTest::class.java.getResource(resource)?.path
        assertNotNull(path)
        return File(path)
    }

    private fun fetcherForFile(file: File): Fetcher = runBlocking {
        Fetcher.fromFile(file.file)
    }

    @Test
    fun `CBZ is accepted`() {
        assertNotNull(parser.parseBlocking(cbzFile, cbzFetcher))
    }

    @Test
    fun `JPG is accepted`() {
        assertNotNull(parser.parseBlocking(jpgFile, jpgFetcher))
    }

    @Test
    fun `readingOrder is sorted alphabetically`() {
        val builder = parser.parseBlocking(cbzFile, cbzFetcher)
        assertNotNull(builder)
        val readingOrder = builder.manifest.readingOrder
            .map { it.href.removePrefix("/Cory Doctorow's Futuristic Tales of the Here and Now") }
        assertThat(readingOrder)
            .containsExactly("/a-fc.jpg", "/x-002.jpg", "/x-003.jpg", "/x-004.jpg")
    }

    @Test
    fun `the cover is the first item in the readingOrder`() {
        val builder = parser.parseBlocking(cbzFile, cbzFetcher)
        assertNotNull(builder)
        with(builder.manifest.readingOrder) {
            assertEquals(
                "/Cory Doctorow's Futuristic Tales of the Here and Now/a-fc.jpg",
                firstWithRel("cover")?.href)
        }
    }

    @Test
    fun `title is based on archive's root directory when any`() {
        val builder = parser.parseBlocking(cbzFile, cbzFetcher)
        assertNotNull(builder)
        assertEquals("Cory Doctorow's Futuristic Tales of the Here and Now", builder.manifest.metadata.title)
    }
}