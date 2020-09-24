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
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfiumCore
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.extensions.md5
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.util.pdf.PdfDocument
import timber.log.Timber
import java.io.File
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

        internal suspend  fun open(resource: Resource, context: Context): PdfiumDocument {
            val core = PdfiumCore(context.applicationContext)
            return resource.use { res ->
                val file = resource.file
                if (file != null) core.fromFile(file)
                else core.fromBytes(resource.read().getOrThrow())
            }
        }

        /**
         * Creates a [PdfiumDocument] from a [File].
         */
        private fun PdfiumCore.fromFile(file: File): PdfiumDocument =
            fromDocument(
                newDocument(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)),
                identifier = file.md5()
            )

        /**
         * Creates a [PdfiumDocument] from raw bytes.
         */
        private fun PdfiumCore.fromBytes(bytes: ByteArray): PdfiumDocument =
            fromDocument(
                newDocument(bytes),
                identifier = bytes.md5()
            )

        private fun PdfiumCore.fromDocument(document: _PdfiumDocument, identifier: String?): PdfiumDocument {
            // FIXME: Extract the identifier from the file, it's not exposed by PdfiumCore
            return PdfiumDocument(
                core = this,
                document = document,
                identifier = identifier,
                pageCount = getPageCount(document)
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

/** Pdfium is the default implementation. */
@PdfSupport
suspend fun PdfDocument.Companion.open(resource: Resource, context: Context): PdfDocument =
    PdfiumDocument.open(resource, context)
