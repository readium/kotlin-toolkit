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
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.util.File
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.OnAskCredentials
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.Archive
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.streamer.extensions.fromFile
import org.readium.r2.streamer.parser.audio.AudioParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.epub.setLayoutStyle
import org.readium.r2.streamer.parser.image.ImageParser
import org.readium.r2.streamer.parser.pdf.PdfiumDocument
import org.readium.r2.streamer.parser.readium.ReadiumWebPubParser
import java.io.FileNotFoundException
import java.lang.Exception

internal typealias PublicationTry<SuccessT> = Try<SuccessT, Publication.OpeningError>

typealias OnCreateManifest = (File, Manifest) -> Manifest

typealias OnCreateFetcher = (File, Manifest, Fetcher) -> Fetcher

typealias OnCreateServices = (File, Manifest, Publication.ServicesBuilder) -> Unit

/**
 *  Opens a Publication using a list of parsers.
 *
 *  @param parsers Parsers used to open a publication, in addition to the default parsers.
 *    The provided parsers take precedence over the default parsers.
 *    This can be used to provide custom parsers, or a different configuration for default parsers.
 *  @param ignoreDefaultParsers When true, only parsers provided in parsers will be used.
 *    Can be used if you want to support only a subset of Readium's parsers.
 *  @param openArchive Opens an archive (e.g. ZIP, RAR), optionally protected by credentials.
 *    The default implementation uses java.zip package and passwords are not supported with it.
 *  @param openPdf Parses a PDF document, optionally protected by password.
 *    The default implementation uses Pdfium.
 *  @param onCreateManifest Called before creating the Publication, to modify the parsed Manifest if desired.
 *  @param onCreateFetcher Called before creating the Publication, to modify its root fetcher.
 *  @param onCreateServices Called before creating the Publication, to modify its list of service factories.
 *  @param onAskCredentials Called when a content protection wants to prompt the user for its credentials.
 *    This is used by ZIP and PDF password protections.
 *    The default implementation of this callback presents a dialog using native components when possible.
 */

@OptIn(PdfSupport::class)
class Streamer constructor(
    context: Context,
    parsers: List<PublicationParser> = emptyList(),
    ignoreDefaultParsers: Boolean = false,
    private val contentProtections: List<ContentProtection> = emptyList(),
    private val openArchive: suspend (String) -> Archive? = (Archive)::open,
    private val openPdf: suspend (Resource) -> PdfDocument? = { PdfiumDocument.open(it, context.applicationContext) },
    private val onCreateManifest: OnCreateManifest = { _, manifest -> manifest },
    private val onCreateFetcher: OnCreateFetcher = { _, _, fetcher -> fetcher },
    private val onCreateServices: OnCreateServices = { _, _, _ -> Unit },
    private val onAskCredentials: OnAskCredentials = { _, _, _ -> Unit }
) {

    private val defaultParsers: List<PublicationParser> by lazy {
        listOf(
            EpubParser(),
            //PdfParser(context.applicationContext, openPdf),
            ReadiumWebPubParser(context.applicationContext),
            ImageParser(),
            AudioParser()
        )
    }

    private val parsers: List<PublicationParser> = parsers +
        if (!ignoreDefaultParsers) defaultParsers else emptyList()

    /**
     * Parses a Publication from the given file.
     *
     * Returns null if the file was not recognized by any parser, or a Streamer.Error in case of failure.
     *
     * @param file Path to the publication file..
     * @param fallbackTitle The Publication's title is mandatory, but some formats might not have a way of declaring a title (e.g. CBZ).
     *   In which case, fallbackTitle will be used.
     *   The default implementation uses the filename as the fallback title.
     * @param askCredentials Indicates whether the user can be prompted for its credentials.
     *   This should be set to true when you want to render a publication in a Navigator.
     *   When false, Content Protections are allowed to do background requests, but not to present a UI to the user.
     * @param credentials Credentials that Content Protections can use to attempt to unlock a publication, for example a password.
     *   Supporting string credentials is entirely optional for Content Protections, this is only provided as a convenience.
     *   The format is free-form: it can be a simple password, or a structured format such as JSON or XML.
     * @param sender
     *   Free object that can be used by reading apps to give some context to the Content Protections.
     *   For example, it could be the source Activity/ViewController which would be used to present a credentials dialog.
     *   Content Protections are not supposed to use this parameter directly.
     *   Instead, it should be forwarded to the reading app if an interaction is needed.
     * @param warnings Logger used to broadcast non-fatal parsing warnings.
     *   Can be used to report publication authoring mistakes,
     *   to warn users of potential rendering issues or help authors debug their publications.
     */
    suspend fun open(
        file: File,
        fallbackTitle: String = file.name,
        askCredentials: Boolean,
        credentials: String? = null,
        sender: Any? = null,
        warnings: WarningLogger? = null
    ): PublicationTry<Publication> = try {

        val baseFetcher = try {
            Fetcher.fromFile(file.file, openArchive)
        } catch (e: SecurityException) {
            throw Publication.OpeningError.Forbidden(e)
        } catch (e: FileNotFoundException) {
            throw Publication.OpeningError.NotFound
        }

        val protectedFile = contentProtections
            .lazyMapFirstNotNullOrNull {
                it.open(
                    file,
                    baseFetcher,
                    askCredentials,
                    credentials,
                    sender,
                    onAskCredentials
                )
            }
            ?.getOrThrow()

        val builder = parsers
            .lazyMapFirstNotNullOrNull {
                try {
                    it.parse(
                        protectedFile?.file ?: file,
                        protectedFile?.fetcher ?: baseFetcher,
                        fallbackTitle,
                        warnings
                    )
                } catch (e: Exception) {
                    throw Publication.OpeningError.ParsingFailed(e)
                }
            } ?: throw Publication.OpeningError.UnsupportedFormat

        builder.apply {
            manifest = onCreateManifest(file, manifest)
            fetcher = onCreateFetcher(file, manifest, fetcher)
            servicesBuilder.apply {
                contentProtectionServiceFactory = protectedFile?.protectionServiceFactory
            }
            onCreateServices(file, manifest, servicesBuilder)
        }

        val publication = builder.build()
            .apply { addLegacyProperties(file.format()) }

        Try.success(publication)

    } catch (e: Publication.OpeningError) {
        Try.failure(e)
    }

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
