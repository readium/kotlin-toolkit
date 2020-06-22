/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.pdf

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.util.File
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.InMemoryCoverService
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.shared.util.pdf.toLinks
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.PublicationParser
import java.lang.Exception

/**
 * Parses a PDF file into a Readium [Publication].
 */
@PdfSupport
class PdfParser(
    private val context: Context,
    private val openPdf:  suspend (ByteArray) -> PdfDocument? = { PdfiumDocument.fromBytes(it, context.applicationContext) }
) : PublicationParser, org.readium.r2.streamer.parser.PublicationParser {

    override suspend fun parse(
        file: File,
        fetcher: Fetcher,
        fallbackTitle: String,
        warnings: WarningLogger?
    ): Try<PublicationParser.PublicationBuilder, Throwable>? {

        if (file.format() != Format.PDF)
            return null

        return try {
            Try.success(makeBuilder(file, fetcher, fallbackTitle))
        } catch (e: Exception) {
            Try.failure(e)
        }
    }

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {

        val file = File(fileAtPath)
        val baseFetcher = FileFetcher(href = "/${file.name}", file = file.file)
        val builder = try {
            makeBuilder(file, baseFetcher, fallbackTitle)
        } catch (e: Exception) {
            return@runBlocking null
        }

        with(builder) {
            val publication = Publication(
                manifest = manifest,
                fetcher = fetcher,
                servicesBuilder = servicesBuilder
            )

            val container = PublicationContainer(
                fetcher = fetcher,
                path = file.file.canonicalPath,
                mediaType = MediaType.PDF
            )

            PubBox(publication, container)
        }
    }

    private suspend fun makeBuilder(file: File, fetcher: Fetcher, fallbackTitle:  String)
            : PublicationParser.PublicationBuilder {

        val fileHref = fetcher.links().firstOrNull { it.mediaType == MediaType.PDF }?.href
            ?: throw Exception("Unable to find PDF file.")
        val fileBytes = fetcher.get(fileHref).read().getOrThrow()
        val document = openPdf(fileBytes)
            ?: throw Exception("Unable to open PDF file.")
        val tableOfContents = document.outline.toLinks(fileHref)

        val manifest = Manifest(
            metadata = Metadata(
                identifier = document.identifier ?: file.name,
                localizedTitle = LocalizedString(document.title?.ifBlank { null } ?: fallbackTitle),
                authors = listOfNotNull(document.author).map { Contributor(name = it) },
                numberOfPages = document.pageCount
            ),
            readingOrder = listOf(Link(href = fileHref, type = MediaType.PDF.toString())),
            tableOfContents = tableOfContents
        )

        val servicesBuilder = Publication.ServicesBuilder(
            positions = (PdfPositionsService)::create,
            cover = document.cover?.let { InMemoryCoverService.createFactory(it) }
        )

        return PublicationParser.PublicationBuilder(manifest, fetcher, servicesBuilder)
    }

}
