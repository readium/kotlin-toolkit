/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.firstWithRel
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.file.FileResourceFactory
import org.readium.r2.shared.util.format.ArchiveSniffer
import org.readium.r2.shared.util.format.BitmapSniffer
import org.readium.r2.shared.util.format.CompositeFormatSniffer
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.format.ZipSniffer
import org.readium.r2.shared.util.fromFilePath
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.zip.ZipArchiveOpener
import org.readium.r2.streamer.Fixtures

class ImageParserTest {

    private val fixtures = Fixtures("image")

    private val archiveOpener = ZipArchiveOpener()

    private val assetSniffer = AssetRetriever(
        FileResourceFactory(),
        archiveOpener,
        CompositeFormatSniffer(ZipSniffer, ArchiveSniffer, BitmapSniffer)
    )

    private val parser = ImageParser(assetSniffer)

    private suspend fun cbzAsset(): ContainerAsset {
        val resource = FileResource(urlForFixture("futuristic_tales.cbz"))
        val format = Format(
            specification = FormatSpecification(Specification.Zip, Specification.InformalComic),
            mediaType = MediaType.CBZ,
            fileExtension = FileExtension("cbz")
        )
        val archive = archiveOpener.open(format, resource).checkSuccess()
        return ContainerAsset(format, archive.container)
    }

    private fun jpgAsset(): ResourceAsset {
        val resource = FileResource(urlForFixture("futuristic_tales.jpg"))
        val format = Format(
            specification = FormatSpecification(Specification.Jpeg),
            mediaType = MediaType.JPEG,
            fileExtension = FileExtension("jpg")
        )
        return ResourceAsset(format, resource)
    }

    private fun urlForFixture(name: String): AbsoluteUrl =
        checkNotNull(AbsoluteUrl.fromFilePath(fixtures.path(name).toString()))

    private suspend fun parse(asset: Asset): Publication.Builder? =
        parser.parse(asset).getOrNull()

    @Test
    fun `CBZ is accepted`() = runTest {
        assertNotNull(parse(cbzAsset()))
    }

    @Test
    fun `JPG is accepted`() = runTest {
        assertNotNull(parse(jpgAsset()))
    }

    @Test
    fun `conformsTo contains the Divina profile`() = runTest {
        val manifest = parse(cbzAsset())?.manifest
        assertEquals(setOf(Publication.Profile.DIVINA), manifest?.metadata?.conformsTo)
    }

    @Test
    fun `readingOrder is sorted alphabetically`() = runTest {
        val builder = assertNotNull(parse(cbzAsset()))
        val base = Url.fromDecodedPath("Cory Doctorow's Futuristic Tales of the Here and Now/")!!
        val readingOrder = builder.manifest.readingOrder
            .map { base.relativize(it.url()).toString() }
        assertEquals(
            listOf("a-fc.jpg", "x-002.jpg", "x-003.jpg", "x-004.jpg"),
            readingOrder
        )
    }

    @Test
    fun `the cover is the first item in the readingOrder`() = runTest {
        val builder = assertNotNull(parse(cbzAsset()))
        with(builder.manifest.readingOrder) {
            assertEquals(
                Url.fromDecodedPath("Cory Doctorow's Futuristic Tales of the Here and Now/a-fc.jpg"),
                firstWithRel("cover")?.url()
            )
        }
    }

    @Test
    fun `title is null for archives`() = runTest {
        val builder = assertNotNull(parse(cbzAsset()))
        assertNull(builder.manifest.metadata.title)
    }
}
