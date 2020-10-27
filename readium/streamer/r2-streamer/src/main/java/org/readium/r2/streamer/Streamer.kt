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
import org.readium.r2.shared.util.File
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.DefaultArchiveFactory
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.streamer.extensions.fromFile
import org.readium.r2.streamer.parser.audio.AudioParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.epub.setLayoutStyle
import org.readium.r2.streamer.parser.image.ImageParser
import org.readium.r2.streamer.parser.pdf.PdfParser
import org.readium.r2.streamer.parser.pdf.PdfiumPdfDocumentFactory
import org.readium.r2.streamer.parser.readium.ReadiumWebPubParser
import java.io.FileNotFoundException
import java.lang.Exception

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
 * @param onCreatePublication Called on every parsed [Publication.Builder]. It can be used to modify
 *   the [Manifest], the root [Fetcher] or the list of service factories of a [Publication].
 */
@OptIn(PdfSupport::class)
class Streamer constructor(
    context: Context,
    parsers: List<PublicationParser> = emptyList(),
    ignoreDefaultParsers: Boolean = false,
    private val contentProtections: List<ContentProtection> = emptyList(),
    private val archiveFactory: ArchiveFactory = DefaultArchiveFactory(),
    private val pdfFactory: PdfDocumentFactory = DefaultPdfDocumentFactory(context),
    private val onCreatePublication: Publication.Builder.() -> Unit = {}
) {

    /**
     * Parses a [Publication] from the given file.
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
     * @param file Path to the publication file..
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
     * @return Null if the file was not recognized by any parser, or a [Publication.OpeningException]
     *   in case of failure.
     */
    suspend fun open(
        file: File,
        credentials: String? = null,
        allowUserInteraction: Boolean,
        sender: Any? = null,
        onCreatePublication: Publication.Builder.() -> Unit = {},
        warnings: WarningLogger? = null
    ): PublicationTry<Publication> = try {

        @Suppress("NAME_SHADOWING")
        var file = file
        var fetcher = try {
            Fetcher.fromFile(file.file, archiveFactory)
        } catch (e: SecurityException) {
            throw Publication.OpeningException.Forbidden(e)
        } catch (e: FileNotFoundException) {
            throw Publication.OpeningException.NotFound
        }

        val protectedFile = contentProtections
            .lazyMapFirstNotNullOrNull {
                it.open(file, fetcher, credentials, allowUserInteraction, sender)
            }
            ?.getOrThrow()

        if (protectedFile != null) {
            file = protectedFile.file
            fetcher = protectedFile.fetcher
        }

        val builder = parsers
            .lazyMapFirstNotNullOrNull {
                try {
                    it.parse(
                        file,
                        fetcher,
                        warnings
                    )
                } catch (e: Exception) {
                    throw Publication.OpeningException.ParsingFailed(e)
                }
            } ?: throw Publication.OpeningException.UnsupportedFormat

        // Transform from the Content Protection.
        protectedFile?.let { builder.apply(it.onCreatePublication) }
        // Transform provided by the reading app during the construction of the Streamer.
        builder.apply(this.onCreatePublication)
        // Transform provided by the reading app in `Streamer.open()`.
        builder.apply(onCreatePublication)

        val publication = builder
            .apply(onCreatePublication)
            .build()
            .apply { addLegacyProperties(file.format()) }

        Try.success(publication)

    } catch (e: Publication.OpeningException) {
        Try.failure(e)
    }

    private val defaultParsers: List<PublicationParser> by lazy {
        listOf(
            EpubParser(),
            PdfParser(context, pdfFactory),
            ReadiumWebPubParser(pdfFactory),
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

    private fun Publication.addLegacyProperties(format: Format?) {
        type = format.toPublicationType()

        if (format == Format.EPUB)
            setLayoutStyle()
    }
}

internal fun Format?.toPublicationType(): Publication.TYPE =
    when (this) {
        Format.READIUM_AUDIOBOOK, Format.READIUM_AUDIOBOOK_MANIFEST, Format.LCP_PROTECTED_AUDIOBOOK -> Publication.TYPE.AUDIO
        Format.DIVINA, Format.DIVINA_MANIFEST -> Publication.TYPE.DiViNa
        Format.CBZ -> Publication.TYPE.CBZ
        Format.EPUB -> Publication.TYPE.EPUB
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
