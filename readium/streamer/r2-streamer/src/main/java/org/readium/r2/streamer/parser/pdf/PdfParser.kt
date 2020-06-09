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
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.util.pdf.toLinks
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.InMemoryCoverService
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber
import java.io.File

/**
 * Parses a PDF file into a Readium [Publication].
 */
class PdfParser(private val context: Context) : PublicationParser {

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? =
        try {
            val file = File(fileAtPath)
            val rootHref = "/publication.pdf"
            val document = PdfiumDocument.fromBytes(File(fileAtPath).readBytes(), context)
            val tableOfContents = document.outline.toLinks(rootHref)

            val publication = Publication(
                manifest = Manifest(
                    metadata = Metadata(
                        identifier = document.identifier ?: file.name,
                        localizedTitle = LocalizedString(document.title?.ifEmpty { null } ?: file.toTitle()),
                        authors = listOfNotNull(document.author).map { Contributor(name = it) },
                        numberOfPages = document.pageCount
                    ),
                    readingOrder = listOf(Link(href = rootHref, type = MediaType.PDF.toString())),
                    tableOfContents = tableOfContents
                ),
                fetcher = FileFetcher(href = rootHref, file = file),
                servicesBuilder = Publication.ServicesBuilder(
                    positions = (PdfPositionsService)::create,
                    cover = document.cover?.let { InMemoryCoverService.create(it) }
                )
            )

            val container = PublicationContainer(
                publication = publication,
                path = fileAtPath,
                mediaType = MediaType.PDF
            )

            PubBox(publication, container)

        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    private fun File.toTitle(): String =
        nameWithoutExtension.replace("_", " ")

}
