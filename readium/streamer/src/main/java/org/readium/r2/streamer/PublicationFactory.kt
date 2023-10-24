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
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.parser.audio.AudioParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.image.ImageParser
import org.readium.r2.streamer.parser.pdf.PdfParser
import org.readium.r2.streamer.parser.readium.ReadiumWebPubParser

internal typealias PublicationTry<SuccessT> = Try<SuccessT, Publication.OpenError>

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
    mediaTypeRetriever: MediaTypeRetriever,
    httpClient: HttpClient,
    pdfFactory: PdfDocumentFactory<*>?,
    private val onCreatePublication: Publication.Builder.() -> Unit = {}
) {

    public companion object {
        public operator fun invoke(
            context: Context,
            contentProtections: List<ContentProtection> = emptyList(),
            onCreatePublication: Publication.Builder.() -> Unit
        ): PublicationFactory {
            val mediaTypeRetriever = MediaTypeRetriever()
            return PublicationFactory(
                context = context,
                contentProtections = contentProtections,
                formatRegistry = FormatRegistry(),
                mediaTypeRetriever = mediaTypeRetriever,
                httpClient = DefaultHttpClient(mediaTypeRetriever),
                pdfFactory = null,
                onCreatePublication = onCreatePublication
            )
        }
    }

    private val contentProtections: Map<ContentProtection.Scheme, ContentProtection> =
        buildList {
            add(LcpFallbackContentProtection(mediaTypeRetriever))
            add(AdeptFallbackContentProtection())
            addAll(contentProtections.asReversed())
        }.associateBy(ContentProtection::scheme)

    private val defaultParsers: List<PublicationParser> =
        listOfNotNull(
            EpubParser(mediaTypeRetriever),
            pdfFactory?.let { PdfParser(context, it) },
            ReadiumWebPubParser(context, pdfFactory, mediaTypeRetriever),
            ImageParser(),
            AudioParser()
        )

    private val parsers: List<PublicationParser> = parsers +
        if (!ignoreDefaultParsers) defaultParsers else emptyList()

    private val parserAssetFactory: ParserAssetFactory =
        ParserAssetFactory(httpClient, mediaTypeRetriever, formatRegistry)

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
     * @return A [Publication] or a [Publication.OpenError] in case of failure.
     */
    public suspend fun open(
        asset: Asset,
        contentProtectionScheme: ContentProtection.Scheme? = null,
        credentials: String? = null,
        allowUserInteraction: Boolean,
        onCreatePublication: Publication.Builder.() -> Unit = {},
        warnings: WarningLogger? = null
    ): PublicationTry<Publication> {
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
    ): Try<Publication, Publication.OpenError> {
        val parserAsset = parserAssetFactory.createParserAsset(asset)
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
    ): Try<Publication, Publication.OpenError> {
        val protectedAsset = contentProtections[contentProtectionScheme]
            ?.open(asset, credentials, allowUserInteraction)
            ?.getOrElse { return Try.failure(it) }
            ?: return Try.failure(Publication.OpenError.Forbidden())

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
    ): Try<Publication, Publication.OpenError> {
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

    private fun wrapParserException(e: PublicationParser.Error): Publication.OpenError =
        when (e) {
            is PublicationParser.Error.FormatNotSupported ->
                Publication.OpenError.UnsupportedAsset("Cannot find a parser for this asset")
            is PublicationParser.Error.IO ->
                when (e.resourceError) {
                    is Resource.Exception.BadRequest, is Resource.Exception.Other ->
                        Publication.OpenError.Unknown(e)
                    is Resource.Exception.Forbidden ->
                        Publication.OpenError.Forbidden(e)
                    is Resource.Exception.NotFound ->
                        Publication.OpenError.InvalidAsset(e)
                    is Resource.Exception.OutOfMemory ->
                        Publication.OpenError.OutOfMemory(e)
                    is Resource.Exception.Unavailable, is Resource.Exception.Offline ->
                        Publication.OpenError.Unavailable(e)
                }
            is PublicationParser.Error.OutOfMemory ->
                Publication.OpenError.OutOfMemory(e)
            is PublicationParser.Error.ParsingFailed ->
                Publication.OpenError.InvalidAsset(e)
        }
}
