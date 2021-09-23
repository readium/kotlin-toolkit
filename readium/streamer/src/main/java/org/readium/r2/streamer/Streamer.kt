/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer

import android.content.Context
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.DefaultArchiveFactory
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.parser.FallbackContentProtection
import org.readium.r2.streamer.parser.audio.AudioParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.epub.setLayoutStyle
import org.readium.r2.streamer.parser.image.ImageParser
import org.readium.r2.streamer.parser.pdf.PdfParser
import org.readium.r2.streamer.parser.pdf.PdfiumPdfDocumentFactory
import org.readium.r2.streamer.parser.readium.ReadiumWebPubParser

internal typealias PublicationTry<SuccessT> = Try<SuccessT, Publication.OpeningException>

/**
 * Opens a Publication using a list of parsers.
 *
 * The [Streamer] is configured to use Readium's default parsers, which you can bypass using
 * [ignoreDefaultParsers]. However, you can provide additional [parsers] which will take precedence
 * over the default ones. This can also be used to provide an alternative configuration of a
 * default parser.
 *
 * @param context Application context.
 * @param parsers Parsers used to open a publication, in addition to the default parsers.
 * @param ignoreDefaultParsers When true, only parsers provided in parsers will be used.
 * @param archiveFactory Opens an archive (e.g. ZIP, RAR), optionally protected by credentials.
 * @param pdfFactory Parses a PDF document, optionally protected by password.
 * @param httpClient Service performing HTTP requests.
 * @param onCreatePublication Called on every parsed [Publication.Builder]. It can be used to modify
 *   the [Manifest], the root [Fetcher] or the list of service factories of a [Publication].
 */
@OptIn(PdfSupport::class)
class Streamer constructor(
    context: Context,
    parsers: List<PublicationParser> = emptyList(),
    ignoreDefaultParsers: Boolean = false,
    contentProtections: List<ContentProtection> = emptyList(),
    private val archiveFactory: ArchiveFactory = DefaultArchiveFactory(),
    private val pdfFactory: PdfDocumentFactory = DefaultPdfDocumentFactory(context),
    private val httpClient: DefaultHttpClient = DefaultHttpClient(),
    private val onCreatePublication: Publication.Builder.() -> Unit = {}
) {

    private val contentProtections: List<ContentProtection> =
        contentProtections + listOf(FallbackContentProtection())

    /**
     * Parses a [Publication] from the given asset.
     *
     * If you are opening the publication to render it in a Navigator, you must set [allowUserInteraction]
     * to true to prompt the user for its credentials when the publication is protected. However,
     * set it to false if you just want to import the [Publication] without reading its content, to
     * avoid prompting the user.
     *
     * When using Content Protections, you can use [sender] to provide a free object which can be
     * used to give some context. For example, it could be the source Activity or Fragment which
     * would be used to present a credentials dialog.
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
     * @param sender Free object that can be used by reading apps to give some UX context when
     *   presenting dialogs.
     * @param onCreatePublication Transformation which will be applied on the Publication Builder.
     *   It can be used to modify the [Manifest], the root [Fetcher] or the list of service
     *   factories of the [Publication].
     * @param warnings Logger used to broadcast non-fatal parsing warnings.
     * @return Null if the asset was not recognized by any parser, or a
     *   [Publication.OpeningException] in case of failure.
     */
    suspend fun open(
        asset: PublicationAsset,
        credentials: String? = null,
        allowUserInteraction: Boolean,
        sender: Any? = null,
        onCreatePublication: Publication.Builder.() -> Unit = {},
        warnings: WarningLogger? = null
    ): PublicationTry<Publication> = try {

        @Suppress("NAME_SHADOWING")
        var asset = asset
        var fetcher = asset.createFetcher(PublicationAsset.Dependencies(archiveFactory = archiveFactory), credentials = credentials)
            .getOrThrow()

        val protectedAsset = contentProtections
            .lazyMapFirstNotNullOrNull {
                it.open(asset, fetcher, credentials, allowUserInteraction, sender)
            }
            ?.getOrThrow()

        if (protectedAsset != null) {
            asset = protectedAsset.asset
            fetcher = protectedAsset.fetcher
        }

        val builder = parsers
            .lazyMapFirstNotNullOrNull {
                try {
                    it.parse(asset, fetcher, warnings)
                } catch (e: Exception) {
                    throw Publication.OpeningException.ParsingFailed(e)
                }
            } ?: throw Publication.OpeningException.UnsupportedFormat(Exception("Cannot find a parser for this asset"))

        // Transform from the Content Protection.
        protectedAsset?.let { builder.apply(it.onCreatePublication) }
        // Transform provided by the reading app during the construction of the Streamer.
        builder.apply(this.onCreatePublication)
        // Transform provided by the reading app in `Streamer.open()`.
        builder.apply(onCreatePublication)

        val publication = builder
            .apply(onCreatePublication)
            .build()
            .apply { addLegacyProperties(asset.mediaType()) }

        Try.success(publication)

    } catch (e: Publication.OpeningException) {
        Try.failure(e)
    }

    private val defaultParsers: List<PublicationParser> by lazy {
        listOf(
            EpubParser(),
            PdfParser(context, pdfFactory),
            ReadiumWebPubParser(pdfFactory, httpClient),
            ImageParser(),
            AudioParser()
        )
    }

    private val parsers: List<PublicationParser> = parsers +
        if (!ignoreDefaultParsers) defaultParsers else emptyList()

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T, R> List<T>.lazyMapFirstNotNullOrNull(transform: suspend (T) -> R): R? {
        for (it in this) {
            return transform(it) ?: continue
        }
        return null
    }

    private fun Publication.addLegacyProperties(mediaType: MediaType?) {
        type = mediaType.toPublicationType()

        if (mediaType == MediaType.EPUB)
            setLayoutStyle()
    }
}

internal fun MediaType?.toPublicationType(): Publication.TYPE =
    when (this) {
        MediaType.READIUM_AUDIOBOOK, MediaType.READIUM_AUDIOBOOK_MANIFEST, MediaType.LCP_PROTECTED_AUDIOBOOK -> Publication.TYPE.AUDIO
        MediaType.DIVINA, MediaType.DIVINA_MANIFEST -> Publication.TYPE.DiViNa
        MediaType.CBZ -> Publication.TYPE.CBZ
        MediaType.EPUB -> Publication.TYPE.EPUB
        else -> Publication.TYPE.WEBPUB
    }

@PdfSupport
class DefaultPdfDocumentFactory private constructor (
    private val factory: PdfDocumentFactory
) : PdfDocumentFactory by factory {

    /** Pdfium is the default implementation. */
    constructor(context: Context)
        : this(PdfiumPdfDocumentFactory(context.applicationContext))

}
