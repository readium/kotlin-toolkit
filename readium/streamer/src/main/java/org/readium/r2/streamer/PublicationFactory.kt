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
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.DefaultMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.zip.ZipArchiveFactory
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
    private val mediaTypeRetriever: MediaTypeRetriever,
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
            override val cause: Error?
        ) : OpenError("Asset is not supported.", cause)

        public class ContentProtectionNotSupported(
            override val cause: Error? = null
        ) : OpenError("No ContentProtection available to open asset.", cause)
    }

    public companion object {
        public operator fun invoke(
            context: Context,
            contentProtections: List<ContentProtection> = emptyList(),
            onCreatePublication: Publication.Builder.() -> Unit
        ): PublicationFactory {
            val mediaTypeSniffer =
                DefaultMediaTypeSniffer()

            val archiveFactory =
                ZipArchiveFactory()

            val formatRegistry =
                FormatRegistry()

            val mediaTypeRetriever =
                MediaTypeRetriever(
                    mediaTypeSniffer,
                    FormatRegistry(),
                    archiveFactory
                )

            return PublicationFactory(
                context = context,
                contentProtections = contentProtections,
                mediaTypeRetriever = mediaTypeRetriever,
                formatRegistry = formatRegistry,
                httpClient = DefaultHttpClient(),
                pdfFactory = null,
                onCreatePublication = onCreatePublication
            )
        }
    }

    private val contentProtections: Map<ContentProtection.Scheme, ContentProtection> =
        buildList {
            add(LcpFallbackContentProtection())
            add(AdeptFallbackContentProtection())
            addAll(contentProtections.asReversed())
        }.associateBy(ContentProtection::scheme)

    private val defaultParsers: List<PublicationParser> =
        listOfNotNull(
            EpubParser(),
            pdfFactory?.let { PdfParser(context, it) },
            ReadiumWebPubParser(context, httpClient, pdfFactory),
            ImageParser(mediaTypeRetriever),
            AudioParser(mediaTypeRetriever)
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
     * @param contentProtectionScheme Scheme of the [ContentProtection] protecting the publication,
     *   or null if there is none.
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
        contentProtectionScheme: ContentProtection.Scheme? = null,
        credentials: String? = null,
        allowUserInteraction: Boolean,
        onCreatePublication: Publication.Builder.() -> Unit = {},
        warnings: WarningLogger? = null
    ): Try<Publication, OpenError> {
        val compositeOnCreatePublication: Publication.Builder.() -> Unit = {
            this@PublicationFactory.onCreatePublication(this)
            onCreatePublication(this)
        }

        return if (contentProtectionScheme == null) {
            openUnprotected(
                asset,
                compositeOnCreatePublication,
                warnings
            )
        } else {
            openProtected(
                asset,
                contentProtectionScheme,
                credentials,
                allowUserInteraction,
                compositeOnCreatePublication,
                warnings
            )
        }
    }

    private suspend fun openUnprotected(
        asset: Asset,
        onCreatePublication: Publication.Builder.() -> Unit,
        warnings: WarningLogger?
    ): Try<Publication, OpenError> {
        val parserAsset = parserAssetFactory.createParserAsset(asset)
            .mapFailure {
                when (it) {
                    is ParserAssetFactory.CreateError.Reading ->
                        OpenError.Reading(it.cause)
                    is ParserAssetFactory.CreateError.FormatNotSupported ->
                        OpenError.FormatNotSupported(it.cause)
                }
            }
            .getOrElse { return Try.failure(it) }
        return openParserAsset(parserAsset, onCreatePublication, warnings)
    }

    private suspend fun openProtected(
        asset: Asset,
        contentProtectionScheme: ContentProtection.Scheme,
        credentials: String?,
        allowUserInteraction: Boolean,
        onCreatePublication: Publication.Builder.() -> Unit,
        warnings: WarningLogger?
    ): Try<Publication, OpenError> {
        val protectedAsset = contentProtections[contentProtectionScheme]
            ?.open(asset, credentials, allowUserInteraction)
            ?.mapFailure {
                when (it) {
                    is ContentProtection.OpenError.Reading ->
                        OpenError.Reading(it.cause)
                    is ContentProtection.OpenError.AssetNotSupported ->
                        OpenError.FormatNotSupported(it)
                }
            }
            ?.getOrElse { return Try.failure(it) }
            ?: return Try.failure(OpenError.ContentProtectionNotSupported())

        val parserAsset = PublicationParser.Asset(
            protectedAsset.mediaType,
            protectedAsset.container
        )

        val compositeOnCreatePublication: Publication.Builder.() -> Unit = {
            protectedAsset.onCreatePublication.invoke(this)
            onCreatePublication(this)
        }

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
