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
import android.graphics.Bitmap
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.pdf.toLinks
import org.readium.r2.shared.publication.*
import org.readium.r2.streamer.container.FileContainer
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Parses a PDF file into a Readium [Publication].
 */
class PdfParser(private val context: Context) : PublicationParser {

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? =
        try {
            val file = File(fileAtPath)

            val rootHref = "/publication.pdf"
            val container = FileContainer(path = fileAtPath, mimetype = MediaType.PDF.toString())
            container.rootFile.rootFilePath = rootHref
            container.files[rootHref] = FileContainer.File.Path(fileAtPath)

            val document = PdfiumDocument.fromBytes(File(fileAtPath).readBytes(), context)
            val links = mutableListOf<Link>()

            document.cover?.let { cover ->
                val stream = ByteArrayOutputStream()
                if (cover.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    val coverHref = "/cover.png"
                    container.files[coverHref] = FileContainer.File.Bytes(stream.toByteArray())
                    links.add(Link(href = coverHref, rels = setOf("cover"), type = MediaType.PNG.toString(), width = cover.width, height = cover.height))
                }
            }

            val publication = Publication(
                metadata = Metadata(
                    identifier = document.identifier ?: file.name,
                    localizedTitle = LocalizedString(document.title?.ifEmpty { null } ?: file.toTitle()),
                    authors = listOfNotNull(document.author).map { Contributor(name = it) },
                    numberOfPages = document.pageCount
                ),
                readingOrder = listOf(Link(href = rootHref, type = MediaType.PDF.toString())),
                links = links,
                tableOfContents = document.outline.toLinks(rootHref)
            )

            PubBox(publication, container)

        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    private fun File.toTitle(): String =
        nameWithoutExtension.replace("_", " ")

}
