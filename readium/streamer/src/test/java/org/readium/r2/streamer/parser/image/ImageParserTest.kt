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
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.firstWithRel
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.AssetSniffer
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.zip.ZipArchiveOpener
import org.readium.r2.streamer.parseBlocking
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageParserTest {

    private val archiveOpener = ZipArchiveOpener()

    private val assetSniffer = AssetSniffer()

    private val formatRegistry = FormatRegistry()

    private val parser = ImageParser(assetSniffer, formatRegistry)

    private val cbzAsset = runBlocking {
        val file = fileForResource("futuristic_tales.cbz")
        val resource = FileResource(file)
        val archive = archiveOpener.open(Format.ZIP, resource).checkSuccess()
        ContainerAsset(Format.CBZ, archive.container)
    }

    private val jpgAsset = runBlocking {
        val file = fileForResource("futuristic_tales.jpg")
        val resource = FileResource(file, mediaType = MediaType.JPEG)
        ResourceAsset(
            Format.JPEG,
            resource
        )
    }

    private fun fileForResource(resource: String): File {
        val path = ImageParserTest::class.java.getResource(resource)?.path
        return File(requireNotNull(path))
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
        val base = Url.fromDecodedPath("Cory Doctorow's Futuristic Tales of the Here and Now/")!!
        val readingOrder = builder!!.manifest.readingOrder
            .map { base.relativize(it.url()).toString() }
        assertThat(readingOrder)
            .containsExactly("a-fc.jpg", "x-002.jpg", "x-003.jpg", "x-004.jpg")
    }

    @Test
    fun `the cover is the first item in the readingOrder`() {
        val builder = parser.parseBlocking(cbzAsset)
        assertNotNull(builder)
        with(builder!!.manifest.readingOrder) {
            assertEquals(
                Url.fromDecodedPath("Cory Doctorow's Futuristic Tales of the Here and Now/a-fc.jpg"),
                firstWithRel("cover")?.url()
            )
        }
    }

    @Test
    fun `title is based on archive's root directory when any`() {
        val builder = parser.parseBlocking(cbzAsset)
        assertNotNull(builder)
        assertEquals(
            "Cory Doctorow's Futuristic Tales of the Here and Now",
            builder!!.manifest.metadata.title
        )
    }
}
