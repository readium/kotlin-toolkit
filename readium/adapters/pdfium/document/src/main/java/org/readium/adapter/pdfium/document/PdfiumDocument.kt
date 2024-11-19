/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.pdfium.document

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfDocument as _PdfiumDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.File
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.md5
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadTry
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use
import timber.log.Timber

public class PdfiumDocument(
    @InternalReadiumApi public val core: PdfiumCore,
    @InternalReadiumApi public val document: _PdfiumDocument,
    override val identifier: String?,
    override val pageCount: Int,
) : PdfDocument {

    private val metadata: _PdfiumDocument.Meta by lazy { core.getDocumentMeta(document) }

    override suspend fun cover(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            core.openPage(document, 0)
            val width = core.getPageWidth(document, 0)
            val height = core.getPageHeight(document, 0)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            core.renderPageBitmap(document, bitmap, 0, 0, 0, width, height, false)
            bitmap
        } catch (e: Exception) {
            Timber.e(e)
            null
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            Timber.e(e)
            null
        }
    }

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

    override fun close() {
        tryOrLog {
            core.closeDocument(document)
        }
    }

    public companion object
}

private fun _PdfiumDocument.Bookmark.toOutlineNode(): PdfDocument.OutlineNode =
    PdfDocument.OutlineNode(
        title = title,
        pageNumber = pageIdx.toInt() + 1,
        children = children.map { it.toOutlineNode() }
    )

public class PdfiumDocumentFactory(context: Context) : PdfDocumentFactory<PdfiumDocument> {

    override val documentType: KClass<PdfiumDocument> = PdfiumDocument::class

    private val core by lazy { PdfiumCore(context.applicationContext) }

    override suspend fun open(resource: Resource, password: String?): ReadTry<PdfiumDocument> {
        // First try to open the resource as a file on the FS for performance improvement, as
        // PDFium requires the whole PDF document to be loaded in memory when using raw bytes.
        return resource.openAsFile(password)
            ?: resource.openBytes(password)
    }

    private suspend fun Resource.openAsFile(password: String?): ReadTry<PdfiumDocument>? =
        tryOrNull {
            sourceUrl?.toFile()?.let { file ->
                withContext(Dispatchers.IO) {
                    Try.success(core.fromFile(file, password))
                }
            }
        }

    private suspend fun Resource.openBytes(password: String?): ReadTry<PdfiumDocument> =
        use {
            it.read()
                .flatMap { bytes ->
                    try {
                        Try.success(
                            core.fromBytes(bytes, password)
                        )
                    } catch (e: Exception) {
                        Try.failure(ReadError.Decoding(e))
                    }
                }
        }

    private fun PdfiumCore.fromFile(file: File, password: String?): PdfiumDocument =
        fromDocument(
            newDocument(
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY),
                password
            ),
            identifier = file.md5()
        )

    /**
     * Creates a [PdfiumDocument] from raw bytes.
     */
    private fun PdfiumCore.fromBytes(bytes: ByteArray, password: String?): PdfiumDocument =
        fromDocument(
            newDocument(bytes, password),
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
