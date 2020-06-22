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
import com.shockwave.pdfium.PdfiumCore
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.extensions.md5
import org.readium.r2.shared.pdf.PdfDocument
import timber.log.Timber
import com.shockwave.pdfium.PdfDocument as _PdfiumDocument

@OptIn(PdfSupport::class)
internal class PdfiumDocument private constructor(
    val core: PdfiumCore,
    val document: _PdfiumDocument,
    override val identifier: String?,
    override val pageCount: Int
) : PdfDocument {

    private val metadata: _PdfiumDocument.Meta by lazy { core.getDocumentMeta(document) }

    override val cover: Bitmap? by lazy { core.renderCover(document) }

    override val title: String? get() = metadata.title

    override val author: String? get() = metadata.author

    override val subject: String? get() = metadata.subject

    override val keywords: List<String> get() = metadata.keywords
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    override val outline: List<PdfDocument.OutlineNode> by lazy {
        core.getTableOfContents(document).map { it.toOutlineNode() }
    }

    companion object {

        /**
         * Creates a [PdfiumDocument] from raw bytes.
         *
         * @param href HREF of the PDF document in the [Publication], used to generate the table of
         *        contents.
         */
        fun fromBytes(bytes: ByteArray, context: Context): PdfiumDocument {
            val core = PdfiumCore(context.applicationContext)
            val document = core.newDocument(bytes)

            // FIXME: Extract the identifier from the file, it's not exposed by PdfiumCore
            return PdfiumDocument(
                core = core,
                document = document,
                identifier = bytes.md5(),
                pageCount = core.getPageCount(document)
            )
        }

    }

}

private fun PdfiumCore.renderCover(document: _PdfiumDocument): Bitmap? {
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

@OptIn(PdfSupport::class)
private fun _PdfiumDocument.Bookmark.toOutlineNode(): PdfDocument.OutlineNode =
    PdfDocument.OutlineNode(
        title = title,
        pageNumber = pageIdx.toInt() + 1,
        children = children.map { it.toOutlineNode() }
    )
