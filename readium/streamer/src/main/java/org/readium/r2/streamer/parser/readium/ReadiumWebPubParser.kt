/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.readium

import android.content.Context
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.shared.publication.services.cacheServiceFactory
import org.readium.r2.shared.publication.services.locatorServiceFactory
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.extensions.readAsJsonOrNull
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.parser.audio.AudioLocatorService

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 */
class ReadiumWebPubParser(
    private val context: Context? = null,
    private val pdfFactory: PdfDocumentFactory<*>?,
) : PublicationParser {

    override suspend fun parse(
        asset: PublicationParser.Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {

        if (!asset.mediaType.isReadiumWebPublication)
            return Try.failure(PublicationParser.Error.FormatNotSupported())

        val manifestJson = asset.fetcher
            .get("/manifest.json")
            .readAsJson()
            .getOrElse { return Try.failure(PublicationParser.Error.IO(it)) }

        val manifest = Manifest.fromJSON(manifestJson, packaged = !asset.mediaType.isRwpm)
            ?: return Try.failure(
                PublicationParser.Error.ParsingFailed("Failed to parse the RWPM Manifest")
            )

        // Checks the requirements from the LCPDF specification.
        // https://readium.org/lcp-specs/notes/lcp-for-pdf.html
        val readingOrder = manifest.readingOrder
        if (asset.mediaType == MediaType.LCP_PROTECTED_PDF &&
            (readingOrder.isEmpty() || !readingOrder.all { it.mediaType.matches(MediaType.PDF) })
        ) {
            return Try.failure(PublicationParser.Error.ParsingFailed("Invalid LCP Protected PDF."))
        }

        val servicesBuilder = Publication.ServicesBuilder().apply {
            cacheServiceFactory = InMemoryCacheService.createFactory(context)

            when (asset.mediaType) {
                MediaType.LCP_PROTECTED_PDF ->
                    positionsServiceFactory = pdfFactory?.let { LcpdfPositionsService.create(it) }

                MediaType.DIVINA ->
                    positionsServiceFactory = PerResourcePositionsService.createFactory("image/*")

                MediaType.READIUM_AUDIOBOOK, MediaType.LCP_PROTECTED_AUDIOBOOK ->
                    locatorServiceFactory = AudioLocatorService.createFactory()
            }
        }

        val publicationBuilder = Publication.Builder(manifest, asset.fetcher, servicesBuilder)
        return Try.success(publicationBuilder)
    }
}

/** Returns whether this media type is of a Readium Web Publication profile. */
private val MediaType.isReadiumWebPublication: Boolean get() = matchesAny(
    MediaType.READIUM_WEBPUB, MediaType.DIVINA, MediaType.LCP_PROTECTED_PDF,
    MediaType.READIUM_AUDIOBOOK, MediaType.LCP_PROTECTED_AUDIOBOOK,
)
