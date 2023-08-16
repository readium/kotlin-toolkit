/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.pdf

import android.content.Context
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.publication.services.InMemoryCoverService
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.pdf.toLinks
import org.readium.r2.streamer.extensions.toLink
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses a PDF file into a Readium [Publication].
 */
@PdfSupport
@OptIn(ExperimentalReadiumApi::class)
public class PdfParser(
    context: Context,
    private val pdfFactory: PdfDocumentFactory<*>
) : PublicationParser {

    private val context = context.applicationContext

    override suspend fun parse(
        asset: PublicationParser.Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {
        if (asset.mediaType != MediaType.PDF)
            return Try.failure(PublicationParser.Error.FormatNotSupported())

        val resource = asset.container.entries()?.firstOrNull()
            ?: return Try.failure(
                PublicationParser.Error.ParsingFailed("No PDF found in the publication.")
            )
        val document = pdfFactory.open(resource, password = null)
        val tableOfContents = document.outline.toLinks(resource.path)

        val manifest = Manifest(
            metadata = Metadata(
                identifier = document.identifier,
                conformsTo = setOf(Publication.Profile.PDF),
                localizedTitle = document.title?.ifBlank { null }?.let { LocalizedString(it) },
                authors = listOfNotNull(document.author).map { Contributor(name = it) },
                readingProgression = document.readingProgression,
                numberOfPages = document.pageCount,
            ),
            readingOrder = listOf(resource.toLink(MediaType.PDF)),
            tableOfContents = tableOfContents
        )

        val servicesBuilder = Publication.ServicesBuilder(
            cache = InMemoryCacheService.createFactory(context),
            positions = PdfPositionsService.Companion::create,
            cover = document.cover(context)?.let { InMemoryCoverService.createFactory(it) }
        )

        val publicationBuilder = Publication.Builder(manifest, asset.container, servicesBuilder)

        return Try.success(publicationBuilder)
    }
}
