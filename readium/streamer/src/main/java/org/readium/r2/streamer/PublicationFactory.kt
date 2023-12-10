/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import android.content.Context
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.AdeptFallbackContentProtection
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.protection.LcpFallbackContentProtection
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetSniffer
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.format.FormatRegistry
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.parser.audio.AudioParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.image.ImageParser
import org.readium.r2.streamer.parser.pdf.PdfParser
import org.readium.r2.streamer.parser.readium.ReadiumWebPubParser

/**
 * Opens a Publication using a list of parsers.
 *
 * The [PublicationFactory] is configured to use Readium's default parsers, which you can bypass
 * using ignoreDefaultParsers. However, you can provide additional [parsers] which will take
 * precedence over the default ones. This can also be used to provide an alternative configuration
 * of a default parser.
 *
 * @param context Application context.
 * @param parsers Parsers used to open a publication, in addition to the default parsers.
 * @param ignoreDefaultParsers When true, only parsers provided in parsers will be used.
 * @param contentProtections Opens DRM-protected publications.
 * @param httpClient Service performing HTTP requests.
 * @param pdfFactory Parses a PDF document, optionally protected by password.
 * @param onCreatePublication Called on every parsed [Publication.Builder]. It can be used to modify
 *   the manifest, the root container or the list of service factories of a [Publication].
 */
@OptIn(PdfSupport::class)
public class PublicationFactory(
    context: Context,
    parsers: List<PublicationParser> = emptyList(),
    ignoreDefaultParsers: Boolean = false,
    contentProtections: List<ContentProtection>,
    formatRegistry: FormatRegistry,
    private val httpClient: HttpClient,
    pdfFactory: PdfDocumentFactory<*>?,
    assetSniffer: AssetSniffer,
    private val onCreatePublication: Publication.Builder.() -> Unit = {}
) {
    public sealed class OpenError(
        override val message: String,
        override val cause: Error?
    ) : Error {

        public class Reading(
            override val cause: org.readium.r2.shared.util.data.ReadError
        ) : OpenError("An error occurred while trying to read asset.", cause)

        public class FormatNotSupported(
            override val cause: Error? = null
        ) : OpenError("Asset is not supported.", cause)
    }

    private val contentProtections: List<ContentProtection> =
        buildList {
            add(LcpFallbackContentProtection())
            add(AdeptFallbackContentProtection())
            addAll(contentProtections.asReversed())
        }

    private val defaultParsers: List<PublicationParser> =
        listOfNotNull(
            EpubParser(),
            pdfFactory?.let { PdfParser(context, it) },
            ReadiumWebPubParser(context, httpClient, pdfFactory),
            ImageParser(assetSniffer, formatRegistry),
            AudioParser(assetSniffer, formatRegistry)
        )

    private val parsers: List<PublicationParser> = parsers +
        if (!ignoreDefaultParsers) defaultParsers else emptyList()

    private val parserAssetFactory: ParserAssetFactory =
        ParserAssetFactory(httpClient, formatRegistry)

    /**
     * Opens a [Publication] from the given asset.
     *
     * If you are opening the publication to render it in a Navigator, you must set [allowUserInteraction]
     * to true to prompt the user for its credentials when the publication is protected. However,
     * set it to false if you just want to import the [Publication] without reading its content, to
     * avoid prompting the user.
     *
     * The [warnings] logger can be used to observe non-fatal parsing warnings, caused by
     * publication authoring mistakes. This can be useful to warn users of potential rendering
     * issues.
     *
     * @param asset Digital medium (e.g. a file) used to access the publication.
     * @param credentials Credentials that Content Protections can use to attempt to unlock a
     *   publication, for example a password.
     * @param allowUserInteraction Indicates whether the user can be prompted, for example for its
     *   credentials.
     * @param onCreatePublication Transformation which will be applied on the Publication Builder.
     *   It can be used to modify the manifest, the root container or the list of service
     *   factories of the [Publication].
     * @param warnings Logger used to broadcast non-fatal parsing warnings.
     * @return A [Publication] or an [OpenError] in case of failure.
     */
    public suspend fun open(
        asset: Asset,
        credentials: String? = null,
        allowUserInteraction: Boolean,
        onCreatePublication: Publication.Builder.() -> Unit = {},
        warnings: WarningLogger? = null
    ): Try<Publication, OpenError> {
        val compositeOnCreatePublication: Publication.Builder.() -> Unit = {
            this@PublicationFactory.onCreatePublication(this)
            onCreatePublication(this)
        }

        parserAssetFactory.createParserAsset(asset)
            .getOrElse {
                when (it) {
                    is ParserAssetFactory.CreateError.Reading ->
                        return Try.failure(OpenError.Reading(it.cause))
                    is ParserAssetFactory.CreateError.FormatNotSupported ->
                        null
                }
            }
            ?.let { openParserAsset(it, compositeOnCreatePublication, warnings) }

        for (protection in contentProtections) {
            protection.open(asset, credentials, allowUserInteraction)
                .getOrElse {
                    when (it) {
                        is ContentProtection.OpenError.Reading ->
                            return Try.failure(OpenError.Reading(it.cause))
                        is ContentProtection.OpenError.AssetNotSupported ->
                            null
                    }
                }?.let { protectedAsset ->
                    val parserAsset = PublicationParser.Asset(
                        protectedAsset.format,
                        protectedAsset.container
                    )

                    val fullOnCreatePublication: Publication.Builder.() -> Unit = {
                        protectedAsset.onCreatePublication.invoke(this)
                        onCreatePublication(this)
                    }

                    return openParserAsset(parserAsset, fullOnCreatePublication)
                }
        }

        if (asset !is ContainerAsset) {
            return Try.failure(OpenError.FormatNotSupported())
        }

        val parserAsset = PublicationParser.Asset(asset.format, asset.container)


        return openParserAsset(parserAsset, compositeOnCreatePublication, warnings)
    }

    private suspend fun openParserAsset(
        publicationAsset: PublicationParser.Asset,
        onCreatePublication: Publication.Builder.() -> Unit = {},
        warnings: WarningLogger? = null
    ): Try<Publication, OpenError> {
        val builder = parse(publicationAsset, warnings)
            .getOrElse { return Try.failure(wrapParserException(it)) }

        builder.apply(onCreatePublication)

        val publication = builder.build()
        return Try.success(publication)
    }

    private suspend fun parse(
        publicationAsset: PublicationParser.Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {
        for (parser in parsers) {
            val result = parser.parse(publicationAsset, warnings)
            if (
                result is Try.Success ||
                result is Try.Failure && result.value !is PublicationParser.Error.FormatNotSupported
            ) {
                return result
            }
        }
        return Try.failure(PublicationParser.Error.FormatNotSupported())
    }

    private fun wrapParserException(e: PublicationParser.Error): OpenError =
        when (e) {
            is PublicationParser.Error.FormatNotSupported ->
                OpenError.FormatNotSupported(DebugError("Cannot find a parser for this asset."))
            is PublicationParser.Error.Reading ->
                OpenError.Reading(e.cause)
        }
}
