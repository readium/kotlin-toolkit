/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.pspdfkit

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.pspdfkit.annotations.actions.GoToAction
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.OutlineElement
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.PdfDocumentLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import timber.log.Timber
import java.io.File
import org.readium.r2.shared.util.pdf.PdfDocument as ReadiumPdfDocument

@PdfSupport
class PsPdfKitDocumentFactory(context: Context) : PdfDocumentFactory {
    private val context = context.applicationContext

    override suspend fun open(file: File, password: String?): ReadiumPdfDocument =
        open(context, DocumentSource(file.toUri(), password))

    override suspend fun open(resource: Resource, password: String?): ReadiumPdfDocument =
        open(context, DocumentSource(ResourceDataProvider(resource), password))

    private suspend fun open(context: Context, documentSource: DocumentSource): ReadiumPdfDocument =
        withContext(Dispatchers.IO) {
            PsPdfKitDocument(PdfDocumentLoader.openDocument(context, documentSource))
        }
}

@PdfSupport
internal class PsPdfKitDocument(
    internal val document: PdfDocument
) : ReadiumPdfDocument {

    // FIXME: Doesn't seem to be exposed by PSPDFKit.
    override val identifier: String?
        get() = null

    override val pageCount: Int
        get() = document.pageCount

    override suspend fun cover(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val size = document.getPageSize(0)
            document.renderPageToBitmap(context, 0, size.width.toInt(), size.height.toInt())
        } catch (e: Exception) {
            Timber.e(e)
            null
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            Timber.e(e)
            null
        }
    }

    override val title: String?
        get() = document.title

    override val author: String?
        get() = document.pdfMetadata.author

    override val subject: String?
        get() = document.pdfMetadata.subject

    override val keywords: List<String>
        get() = document.pdfMetadata.keywords ?: emptyList()

    override val outline: List<ReadiumPdfDocument.OutlineNode> by lazy {
        document.outline.toOutlineNodes()
    }
}

@PdfSupport
private fun List<OutlineElement>.toOutlineNodes(): List<ReadiumPdfDocument.OutlineNode> =
    map { it.toOutlineNode() }

@PdfSupport
private fun OutlineElement.toOutlineNode(): ReadiumPdfDocument.OutlineNode =
    ReadiumPdfDocument.OutlineNode(
        title = title,
        pageNumber = (action as? GoToAction)?.run { pageIndex + 1 },
        children = children.toOutlineNodes()
    )
