/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.fromFilePath
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.zip.ZipArchiveOpener
import org.readium.r2.streamer.Fixtures

/**
 * Parses a fixture EPUB end-to-end: opens the zip container, parses the package document and
 * builds the [Publication]. This runs on every test platform, including the iOS simulator.
 */
class EpubParserTest {

    private suspend fun parse(): Publication {
        val url = checkNotNull(
            AbsoluteUrl.fromFilePath(
                Fixtures("epub").path("childrens-literature.epub").toString()
            )
        )
        val resource = FileResource(url)
        val format = Format(
            specification = FormatSpecification(Specification.Zip, Specification.Epub),
            mediaType = MediaType.EPUB,
            fileExtension = FileExtension("epub")
        )
        val container = ZipArchiveOpener().open(format, resource).checkSuccess().container
        val builder = EpubParser()
            .parse(ContainerAsset(format, container))
            .checkSuccess()
        return builder.build()
    }

    @Test
    fun `an EPUB parses end-to-end`() = runTest {
        val publication = parse()
        val manifest = publication.manifest

        assertEquals(setOf(Publication.Profile.EPUB), manifest.metadata.conformsTo)
        assertEquals("Children's Literature", manifest.metadata.title)
        assertEquals(
            "A Textbook of Sources for Teachers and Teacher-Training Classes",
            manifest.metadata.localizedSubtitle?.string
        )
        assertEquals("http://www.gutenberg.org/ebooks/25545", manifest.metadata.identifier)
        assertEquals(
            listOf(
                Url("EPUB/cover.xhtml")!!,
                Url("EPUB/nav.xhtml")!!,
                Url("EPUB/s04.xhtml")!!
            ),
            manifest.readingOrder.map { it.url() }
        )
        assertTrue(manifest.tableOfContents.isNotEmpty())

        // The publication can read its own resources.
        val readingOrderResource = assertNotNull(publication.get(manifest.readingOrder[0]))
        val bytes = readingOrderResource.read().checkSuccess()
        assertTrue(bytes.isNotEmpty())
    }
}
