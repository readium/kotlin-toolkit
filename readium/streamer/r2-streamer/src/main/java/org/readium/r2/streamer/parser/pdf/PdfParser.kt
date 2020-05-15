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
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import org.readium.r2.shared.extensions.md5
import org.readium.r2.shared.format.MediaType
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
class PdfParser(context: Context) : PublicationParser {

    private val core = PdfiumCore(context.applicationContext)

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? =
        try {
            val file = File(fileAtPath)

            val rootHref = "/publication.pdf"
            val container = FileContainer(path = fileAtPath, mimetype = MediaType.PDF.toString())
            container.rootFile.rootFilePath = rootHref
            container.files[rootHref] = FileContainer.File.Path(fileAtPath)

            val document = core.newDocument(File(fileAtPath).readBytes())
            val meta = core.getDocumentMeta(document)
            val pageCount = core.getPageCount(document)

            val links = mutableListOf<Link>()

            core.renderCover(document)?.let { cover ->
                val stream = ByteArrayOutputStream()
                if (cover.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    val coverHref = "/cover.png"
                    container.files[coverHref] = FileContainer.File.Bytes(stream.toByteArray())
                    links.add(Link(href = coverHref, rels = setOf("cover"), type = MediaType.PNG.toString(), width = cover.width, height = cover.height))
                }
            }

            val publication = Publication(
                metadata = Metadata(
                    // FIXME: Extract the identifier from the file, it's not exposed by PdfiumCore
                    identifier = file.md5() ?: file.name,
                    localizedTitle = LocalizedString(meta.title?.ifEmpty { null } ?: file.toTitle()),
                    authors = listOfNotNull(meta.author).map { Contributor(name = it) },
                    numberOfPages = pageCount
                ),
                readingOrder = listOf(Link(href = rootHref, type = MediaType.PDF.toString())),
                links = links
            )

            PubBox(publication, container)

        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    private fun PdfiumCore.renderCover(document: PdfDocument): Bitmap? {
        try {
            val pointer = openPage(document, 0)
            if (pointer <= 0) return null

            val width = getPageWidth(document, 0)
            val height = getPageHeight(document, 0)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            renderPageBitmap(document, bitmap, 0, 0, 0, width, height, false)
            return bitmap

        } catch (e: Exception) {
            Timber.e(e)
            return null
        }
    }

    private fun File.toTitle(): String =
        nameWithoutExtension.replace("_", " ")

}
