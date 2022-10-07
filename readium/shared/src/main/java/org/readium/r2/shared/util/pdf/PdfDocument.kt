/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.pdf

import android.content.Context
import android.graphics.Bitmap
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.services.cacheService
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.cache.Cache
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.File
import kotlin.reflect.KClass

@PdfSupport
interface PdfDocumentFactory<T : PdfDocument> {

    /** Class for the type of document this factory produces. */
    val documentType: KClass<T>

    /** Opens a PDF from a [file]. */
    suspend fun open(file: File, password: String?): T

    /** Opens a PDF from a Fetcher resource. */
    suspend fun open(resource: Resource, password: String?): T
}

/**
 * Creates a new factory which caches the results of the wrapped factory into a
 * [PublicationServicesHolder].
 *
 * This will ensure that the PDF documents are only cached as long as  the [Publication] object is
 * around.
 */
@ExperimentalReadiumApi
@PdfSupport
suspend fun <T : PdfDocument> PdfDocumentFactory<T>.cachedIn(holder: PublicationServicesHolder): PdfDocumentFactory<T> {
    val namespace = requireNotNull(documentType.qualifiedName)
    val cache = holder.cacheService?.cacheOf(documentType, namespace) ?: return this
    return CachingPdfDocumentFactory(this, cache)
}

@PdfSupport
private class CachingPdfDocumentFactory<T : PdfDocument>(
    private val factory: PdfDocumentFactory<T>,
    private val cache: Cache<T>
) : PdfDocumentFactory<T> by factory {

    override suspend fun open(file: File, password: String?): T =
        cache.transaction {
            getOrPut(file.path) {
                factory.open(file, password)
            }
        }

    override suspend fun open(resource: Resource, password: String?): T =
        cache.transaction {
            getOrPut(resource.link().href) {
                factory.open(resource, password)
            }
        }
}

/**
 * Represents a PDF document.
 */
@PdfSupport
interface PdfDocument : SuspendingCloseable {

    /**
     * Permanent identifier based on the contents of the file at the time it was originally
     * created.
     */
    val identifier: String? get() = null

    /**
     * Number of pages in the document.
     */
    val pageCount: Int

    /**
     * Default reading progression of the document.
     */
    val readingProgression: ReadingProgression get() = ReadingProgression.AUTO

    /**
     * The first page rendered as a cover.
     */
    suspend fun cover(context: Context): Bitmap? = null

    // Values extracted from the document information dictionary, defined in PDF specification.

    /**
     * The document's title.
     */
    val title: String? get() = null

    /**
     * The name of the person who created the document.
     */
    val author: String? get() = null

    /**
     * The subject of the document.
     */
    val subject: String? get() = null

    /**
     * Keywords associated with the document.
     */
    val keywords: List<String> get() = emptyList()

    /**
     * Outline to build the table of contents.
     */
    val outline: List<OutlineNode> get() = emptyList()

    data class OutlineNode(
        val title: String?,
        val pageNumber: Int?,  // Starts from 1.
        val children: List<OutlineNode>
    )

    // To allow extensions on the Companion object.
    companion object
}

/**
 * Converts a PDF outline to [Link] objects.
 *
 * @param documentHref HREF of the PDF document in the [Publication] to which the links are
 *        relative to.
 */
@PdfSupport
fun List<PdfDocument.OutlineNode>.toLinks(documentHref: String): List<Link> =
    map { it.toLink(documentHref) }

@PdfSupport
fun PdfDocument.OutlineNode.toLink(documentHref: String): Link =
    Link(
        href = "$documentHref#page=$pageNumber",
        type = MediaType.PDF.toString(),
        title = title,
        children = children.toLinks(documentHref)
    )
