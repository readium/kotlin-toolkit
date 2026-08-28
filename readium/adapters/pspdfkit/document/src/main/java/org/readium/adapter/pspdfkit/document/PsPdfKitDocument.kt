/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pspdfkit.document

import android.content.Context
import android.graphics.Bitmap
import com.pspdfkit.annotations.actions.GoToAction
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.OutlineElement
import com.pspdfkit.document.PageBinding
import com.pspdfkit.document.PdfDocument as _PsPdfKitDocument
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.exceptions.InvalidPasswordException
import com.pspdfkit.exceptions.InvalidSignatureException
import java.io.IOException
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadTry
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.resource.Resource
import timber.log.Timber

public class PsPdfKitDocumentFactory(context: Context) : PdfDocumentFactory<PsPdfKitDocument> {
    private val context = context.applicationContext

    override val documentType: KClass<PsPdfKitDocument> = PsPdfKitDocument::class

    override suspend fun open(resource: Resource, password: String?): ReadTry<PsPdfKitDocument> =
        withContext(Dispatchers.IO) {
            val dataProvider = ResourceDataProvider(resource)
            val documentSource = DocumentSource(dataProvider, password)
            try {
                val innerDocument = PdfDocumentLoader.openDocument(context, documentSource)
                Try.success(PsPdfKitDocument(innerDocument))
            } catch (e: InvalidPasswordException) {
                Try.failure(ReadError.Decoding(e))
            } catch (e: InvalidSignatureException) {
                Try.failure(ReadError.Decoding(e))
            } catch (e: IOException) {
                dataProvider.error
                    ?.let { Try.failure(it) }
                    // Not a PDF or corrupted file.
                    ?: Try.failure(ReadError.Decoding(e))
            }
        }
}

public class PsPdfKitDocument(
    public val document: _PsPdfKitDocument,
) : PdfDocument {

    // FIXME: Doesn't seem to be exposed by PSPDFKit.
    override val identifier: String?
        get() = null

    override val pageCount: Int
        get() = document.pageCount

    override val readingProgression: ReadingProgression =
        when (document.pageBinding) {
            PageBinding.UNKNOWN, PageBinding.LEFT_EDGE -> ReadingProgression.LTR
            PageBinding.RIGHT_EDGE -> ReadingProgression.RTL
        }

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

    override val outline: List<PdfDocument.OutlineNode> by lazy {
        document.outline.toOutlineNodes()
    }

    override fun close() {}
}

private fun List<OutlineElement>.toOutlineNodes(): List<PdfDocument.OutlineNode> =
    map { it.toOutlineNode() }

private fun OutlineElement.toOutlineNode(): PdfDocument.OutlineNode =
    PdfDocument.OutlineNode(
        title = title,
        pageNumber = (action as? GoToAction)?.run { pageIndex + 1 },
        children = children.toOutlineNodes()
    )
