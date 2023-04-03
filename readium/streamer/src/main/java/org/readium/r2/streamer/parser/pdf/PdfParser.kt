/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.pdf

import android.content.Context
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.publication.services.InMemoryCoverService
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.pdf.toLinks
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses a PDF file into a Readium [Publication].
 */
@PdfSupport
@OptIn(ExperimentalReadiumApi::class)
class PdfParser(
    context: Context,
    private val pdfFactory: PdfDocumentFactory<*>
) : PublicationParser {

    private val context = context.applicationContext

    override suspend fun parse(asset: PublicationAsset, warnings: WarningLogger?): Publication.Builder? {
        if (asset.mediaType != MediaType.PDF)
            return null

        val fileHref = asset.fetcher.links().firstOrNull { it.mediaType == MediaType.PDF }?.href
            ?: throw Exception("Unable to find PDF file.")
        val document = pdfFactory.open(asset.fetcher.get(fileHref), password = null)
        val tableOfContents = document.outline.toLinks(fileHref)

        val manifest = Manifest(
            metadata = Metadata(
                identifier = document.identifier,
                conformsTo = setOf(Publication.Profile.PDF),
                localizedTitle = LocalizedString(document.title?.ifBlank { null } ?: asset.name),
                authors = listOfNotNull(document.author).map { Contributor(name = it) },
                readingProgression = document.readingProgression,
                numberOfPages = document.pageCount,
            ),
            readingOrder = listOf(Link(href = fileHref, type = MediaType.PDF.toString())),
            tableOfContents = tableOfContents
        )

        val servicesBuilder = Publication.ServicesBuilder(
            cache = InMemoryCacheService.createFactory(context),
            positions = PdfPositionsService.Companion::create,
            cover = document.cover(context)?.let { InMemoryCoverService.createFactory(it) }
        )

        return Publication.Builder(manifest, asset.fetcher, servicesBuilder)
    }
}
