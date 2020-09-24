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
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.pdf.OpenPdfDocument
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.shared.util.pdf.toLinks
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.PublicationParser
import org.readium.r2.streamer.extensions.toTitle
import java.lang.Exception

/**
 * Parses a PDF file into a Readium [Publication].
 */
@PdfSupport
class PdfParser(
    context: Context,
    private val openPdf: OpenPdfDocument = { PdfDocument.open(it, context.applicationContext) }
) : PublicationParser, org.readium.r2.streamer.parser.PublicationParser {

    override suspend fun parse(file: File, fetcher: Fetcher, warnings: WarningLogger?): Publication.Builder? =
        _parse(file, fetcher, file.toTitle())

    suspend fun _parse(file: File, fetcher: Fetcher, fallbackTitle: String): Publication.Builder? {
        if (file.format() != Format.PDF)
            return null

        val fileHref = fetcher.links().firstOrNull { it.mediaType == MediaType.PDF }?.href
            ?: throw Exception("Unable to find PDF file.")
        val document = openPdf(fetcher.get(fileHref))
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

        return Publication.Builder(manifest, fetcher, servicesBuilder)
    }

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {

        val file = File(fileAtPath)
        val baseFetcher = FileFetcher(href = "/${file.name}", file = file.file)
        val builder = try {
            _parse(file, baseFetcher, fallbackTitle)
        } catch (e: Exception) {
            return@runBlocking null
        } ?: return@runBlocking null

        val publication = builder.build()
        val container = PublicationContainer(
            publication = publication,
            path = file.file.canonicalPath,
            mediaType = MediaType.PDF
        )

        PubBox(publication, container)
    }
}
