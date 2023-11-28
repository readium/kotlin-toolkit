/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.readium

import android.content.Context
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.shared.publication.services.cacheServiceFactory
import org.readium.r2.shared.publication.services.locatorServiceFactory
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.DecoderError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.readAsRwpm
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.parser.audio.AudioLocatorService

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 */
public class ReadiumWebPubParser(
    private val context: Context? = null,
    private val pdfFactory: PdfDocumentFactory<*>?
) : PublicationParser {

    override suspend fun parse(
        asset: PublicationParser.Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {
        if (!asset.mediaType.isReadiumWebPublication) {
            return Try.failure(PublicationParser.Error.UnsupportedFormat())
        }

        val manifestResource = asset.container[Url("manifest.json")!!]
            ?: return Try.failure(
                PublicationParser.Error.ReadError(
                    ReadError.Decoding(
                        MessageError("Missing manifest.")
                    )
                )
            )

        val manifest = manifestResource
            .readAsRwpm()
            .getOrElse {
                when (it) {
                    is DecoderError.Read ->
                        return Try.failure(
                            PublicationParser.Error.ReadError(
                                ReadError.Decoding(it.cause)
                            )
                        )

                    is DecoderError.Decoding ->
                        return Try.failure(
                            PublicationParser.Error.ReadError(
                                ReadError.Decoding(
                                    MessageError("Failed to parse the RWPM Manifest.")
                                )
                            )
                        )
                }
            }

        // Checks the requirements from the LCPDF specification.
        // https://readium.org/lcp-specs/notes/lcp-for-pdf.html
        val readingOrder = manifest.readingOrder
        if (asset.mediaType == MediaType.LCP_PROTECTED_PDF &&
            (readingOrder.isEmpty() || !readingOrder.all { MediaType.PDF.matches(it.mediaType) })
        ) {
            return Try.failure(
                PublicationParser.Error.ReadError(
                    ReadError.Decoding("Invalid LCP Protected PDF.")
                )
            )
        }

        val servicesBuilder = Publication.ServicesBuilder().apply {
            cacheServiceFactory = InMemoryCacheService.createFactory(context)

            when (asset.mediaType) {
                MediaType.LCP_PROTECTED_PDF ->
                    positionsServiceFactory = pdfFactory?.let { LcpdfPositionsService.create(it) }

                MediaType.DIVINA ->
                    positionsServiceFactory = PerResourcePositionsService.createFactory(
                        MediaType("image/*")!!
                    )

                MediaType.READIUM_AUDIOBOOK, MediaType.LCP_PROTECTED_AUDIOBOOK ->
                    locatorServiceFactory = AudioLocatorService.createFactory()
            }
        }

        val publicationBuilder = Publication.Builder(manifest, asset.container, servicesBuilder)
        return Try.success(publicationBuilder)
    }
}

/** Returns whether this media type is of a Readium Web Publication profile. */
private val MediaType.isReadiumWebPublication: Boolean get() = matchesAny(
    MediaType.READIUM_WEBPUB,
    MediaType.DIVINA,
    MediaType.LCP_PROTECTED_PDF,
    MediaType.READIUM_AUDIOBOOK,
    MediaType.LCP_PROTECTED_AUDIOBOOK
)
