/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.readium

import android.content.Context
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.*
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.extensions.readAsJsonOrNull
import org.readium.r2.streamer.parser.audio.AudioLocatorService

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 */
class ReadiumWebPubParser(
    private val context: Context? = null,
    private val pdfFactory: PdfDocumentFactory<*>?,
) : PublicationParser {

    override suspend fun parse(asset: PublicationAsset, warnings: WarningLogger?): Publication.Builder? {
        val mediaType = asset.mediaType

        if (!mediaType.isReadiumWebPublication)
            return null

        val manifestJson =
            asset.fetcher.readAsJsonOrNull("/manifest.json")
                ?: throw Exception("Manifest not found")

        val manifest = Manifest.fromJSON(manifestJson, packaged = !mediaType.isRwpm)
            ?: throw Exception("Failed to parse the RWPM Manifest")

        // Checks the requirements from the LCPDF specification.
        // https://readium.org/lcp-specs/notes/lcp-for-pdf.html
        val readingOrder = manifest.readingOrder
        if (mediaType == MediaType.LCP_PROTECTED_PDF &&
            (readingOrder.isEmpty() || !readingOrder.all { it.mediaType.matches(MediaType.PDF) })) {
            throw Exception("Invalid LCP Protected PDF.")
        }

        val servicesBuilder = Publication.ServicesBuilder().apply {
            cacheServiceFactory = InMemoryCacheService.createFactory(context)

            when (mediaType) {
                MediaType.LCP_PROTECTED_PDF ->
                    positionsServiceFactory = pdfFactory?.let { LcpdfPositionsService.create(it) }

                MediaType.DIVINA ->
                    positionsServiceFactory = PerResourcePositionsService.createFactory("image/*")

                MediaType.READIUM_AUDIOBOOK, MediaType.LCP_PROTECTED_AUDIOBOOK ->
                    locatorServiceFactory = AudioLocatorService.createFactory()
            }
        }

        return Publication.Builder(manifest, asset.fetcher, servicesBuilder)
    }
}

/** Returns whether this media type is of a Readium Web Publication profile. */
private val MediaType.isReadiumWebPublication: Boolean get() = matchesAny(
    MediaType.READIUM_WEBPUB, MediaType.DIVINA, MediaType.LCP_PROTECTED_PDF,
    MediaType.READIUM_AUDIOBOOK, MediaType.LCP_PROTECTED_AUDIOBOOK,
)
