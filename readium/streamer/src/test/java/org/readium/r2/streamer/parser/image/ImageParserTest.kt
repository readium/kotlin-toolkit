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
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.zip.ZipArchiveOpener
import org.readium.r2.streamer.parseBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ImageParserTest {

    private val archiveOpener = ZipArchiveOpener()

    private val contentResolver = RuntimeEnvironment.getApplication().contentResolver

    private val assetSniffer = AssetRetriever(contentResolver, DefaultHttpClient())

    private val parser = ImageParser(assetSniffer)

    private val cbzAsset = runBlocking {
        val file = fileForResource("futuristic_tales.cbz")
        val resource = FileResource(file)
        val format = Format(
            specification = FormatSpecification(Specification.Zip, Specification.InformalComic),
            mediaType = MediaType.CBZ,
            fileExtension = FileExtension("cbz")
        )
        val archive = archiveOpener.open(format, resource).checkSuccess()
        ContainerAsset(format, archive.container)
    }

    private val jpgAsset = runBlocking {
        val file = fileForResource("futuristic_tales.jpg")
        val resource = FileResource(file)
        val format = Format(
            specification = FormatSpecification(Specification.Jpeg),
            mediaType = MediaType.JPEG,
            fileExtension = FileExtension("jpg")
        )
        ResourceAsset(format, resource)
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
