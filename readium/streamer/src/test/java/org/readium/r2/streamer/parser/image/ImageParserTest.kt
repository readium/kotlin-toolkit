/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.image

import java.io.File
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.readium.r2.shared.fetcher.ContainerFetcher
import org.readium.r2.shared.fetcher.SingleResourceFetcher
import org.readium.r2.shared.fetcher.withLink
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.firstWithRel
import org.readium.r2.shared.resource.DefaultArchiveFactory
import org.readium.r2.shared.resource.FileResource
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.streamer.parseBlocking
import org.readium.r2.streamer.parser.PublicationParser

class ImageParserTest {

    private val parser = ImageParser()

    private val cbzAsset = runBlocking {
        val path = pathForResource("futuristic_tales.cbz")
        val file = File(path)
        val resource = FileResource(file)
        val archive = DefaultArchiveFactory().create(resource, password = null).getOrNull()!!
        val fetcher = ContainerFetcher(archive, MediaTypeRetriever())
        PublicationParser.Asset(file.name, MediaType.CBZ, fetcher)
    }

    private val jpgAsset = runBlocking {
        val path = pathForResource("futuristic_tales.jpg")
        val file = File(path)
        val resource = FileResource(file).withLink(Link(href = "/image.jpg", type = "image/jpeg"))
        val fetcher = SingleResourceFetcher(resource)
        PublicationParser.Asset(file.name, MediaType.JPEG, fetcher)
    }
    private fun pathForResource(resource: String): String {
        val path = ImageParserTest::class.java.getResource(resource)?.path
        assertNotNull(path)
        return path!!
    }

    @Test
    fun `CBZ is accepted`() {
        assertNotNull(parser.parseBlocking(cbzAsset))
    }

    @Test
    fun `JPG is accepted`() {
        assertNotNull(parser.parseBlocking(jpgAsset))
    }

    @Test
    fun `conformsTo contains the Divina profile`() {
        val manifest = parser.parseBlocking(cbzAsset)?.manifest
        assertEquals(setOf(Publication.Profile.DIVINA), manifest?.metadata?.conformsTo)
    }

    @Test
    fun `readingOrder is sorted alphabetically`() {
        val builder = parser.parseBlocking(cbzAsset)
        assertNotNull(builder)
        val readingOrder = builder!!.manifest.readingOrder
            .map { it.href.removePrefix("/Cory Doctorow's Futuristic Tales of the Here and Now") }
        assertThat(readingOrder)
            .containsExactly("/a-fc.jpg", "/x-002.jpg", "/x-003.jpg", "/x-004.jpg")
    }

    @Test
    fun `the cover is the first item in the readingOrder`() {
        val builder = parser.parseBlocking(cbzAsset)
        assertNotNull(builder)
        with(builder!!.manifest.readingOrder) {
            assertEquals(
                "/Cory Doctorow's Futuristic Tales of the Here and Now/a-fc.jpg",
                firstWithRel("cover")?.href
            )
        }
    }

    @Test
    fun `title is based on archive's root directory when any`() {
        val builder = parser.parseBlocking(cbzAsset)
        assertNotNull(builder)
        assertEquals("Cory Doctorow's Futuristic Tales of the Here and Now", builder!!.manifest.metadata.title)
    }
}
