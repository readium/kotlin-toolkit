/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.pdf

import android.content.Context
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.publication.services.InMemoryCoverService
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.pdf.toLinks
import org.readium.r2.streamer.extensions.toContainer
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses a PDF file into a Readium [Publication].
 */
@OptIn(ExperimentalReadiumApi::class)
public class PdfParser(
    context: Context,
    private val pdfFactory: PdfDocumentFactory<*>,
) : PublicationParser {

    private val context = context.applicationContext

    override suspend fun parse(
        asset: Asset,
        warnings: WarningLogger?,
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        if (asset !is ResourceAsset || !asset.format.conformsTo(Specification.Pdf)) {
            return Try.failure(PublicationParser.ParseError.FormatNotSupported())
        }

        val container = asset
            .toContainer()

        val url = container.entries
            .first()

        val document = pdfFactory.open(container[url]!!, password = null)
            .getOrElse { return Try.failure(PublicationParser.ParseError.Reading(it)) }
        val tableOfContents = document.outline.toLinks(url)

        val manifest = Manifest(
            metadata = Metadata(
                identifier = document.identifier,
                conformsTo = setOf(Publication.Profile.PDF),
                localizedTitle = document.title?.ifBlank { null }?.let { LocalizedString(it) },
                authors = listOfNotNull(document.author).map { Contributor(name = it) },
                readingProgression = document.readingProgression,
                numberOfPages = document.pageCount
            ),
            readingOrder = listOf(Link(href = url, mediaType = MediaType.PDF)),
            tableOfContents = tableOfContents
        )

        val servicesBuilder = Publication.ServicesBuilder(
            cache = InMemoryCacheService.createFactory(context),
            positions = PdfPositionsService.Companion::create,
            cover = document.cover(context)?.let { InMemoryCoverService.createFactory(it) }
        )

        val publicationBuilder = Publication.Builder(manifest, container, servicesBuilder)

        return Try.success(publicationBuilder)
    }
}
