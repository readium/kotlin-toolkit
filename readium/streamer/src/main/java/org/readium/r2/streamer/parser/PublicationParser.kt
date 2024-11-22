/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser

import android.content.Context
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.parser.audio.AudioParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.image.ImageParser
import org.readium.r2.streamer.parser.pdf.PdfParser
import org.readium.r2.streamer.parser.readium.ReadiumWebPubParser

/**
 * Parses a [Publication] from an [Asset].
 */
public interface PublicationParser {

    /**
     * Constructs a [Publication.Builder] to build a [Publication] from a publication asset.
     *
     * @param asset Publication asset.
     * @param warnings Used to report non-fatal parsing warnings, such as publication authoring
     * mistakes. This is useful to warn users of potential rendering issues or help authors
     * debug their publications.
     */
    public suspend fun parse(
        asset: Asset,
        warnings: WarningLogger? = null,
    ): Try<Publication.Builder, ParseError>

    public sealed class ParseError(
        public override val message: String,
        public override val cause: org.readium.r2.shared.util.Error?,
    ) : org.readium.r2.shared.util.Error {

        public class FormatNotSupported :
            ParseError("Asset format not supported.", null)

        public class Reading(override val cause: org.readium.r2.shared.util.data.ReadError) :
            ParseError("An error occurred while trying to read asset.", cause)
    }
}

/**
 * Default implementation of [PublicationParser] handling all the publication formats supported by
 * Readium.
 *
 * @param additionalParsers Parsers used to open a publication, in addition to the default parsers. They take precedence over the default ones.
 * @param httpClient Service performing HTTP requests.
 * @param pdfFactory Parses a PDF document, optionally protected by password.
 * @param assetRetriever Opens assets in case of indirection.
 */
public class DefaultPublicationParser(
    context: Context,
    private val httpClient: HttpClient,
    assetRetriever: AssetRetriever,
    pdfFactory: PdfDocumentFactory<*>?,
    additionalParsers: List<PublicationParser> = emptyList(),
) : PublicationParser by CompositePublicationParser(
    additionalParsers + listOfNotNull(
        EpubParser(),
        pdfFactory?.let { PdfParser(context, it) },
        ReadiumWebPubParser(context, httpClient, pdfFactory),
        ImageParser(assetRetriever),
        AudioParser(assetRetriever)
    )
)

/**
 * A composite [PublicationParser] which tries several parsers until it finds one which supports
 * the asset.
 */
public class CompositePublicationParser(
    private val parsers: List<PublicationParser>,
) : PublicationParser {

    public constructor(vararg parsers: PublicationParser) :
        this(parsers.toList())

    override suspend fun parse(
        asset: Asset,
        warnings: WarningLogger?,
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        for (parser in parsers) {
            val result = parser.parse(asset, warnings)
            if (
                result is Try.Success ||
                result is Try.Failure && result.value !is PublicationParser.ParseError.FormatNotSupported
            ) {
                return result
            }
        }
        return Try.failure(PublicationParser.ParseError.FormatNotSupported())
    }
}
