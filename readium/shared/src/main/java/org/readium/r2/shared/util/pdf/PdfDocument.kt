/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.pdf

import android.content.Context
import android.graphics.Bitmap
import kotlin.reflect.KClass
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.services.cacheService
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.cache.Cache
import org.readium.r2.shared.util.cache.getOrTryPut
import org.readium.r2.shared.util.data.ReadTry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

public interface PdfDocumentFactory<T : PdfDocument> {

    /** Class for the type of document this factory produces. */
    public val documentType: KClass<T>

    /** Opens a PDF from a [resource]. */
    public suspend fun open(resource: Resource, password: String?): ReadTry<T>
}

/**
 * Creates a new factory which caches the results of the wrapped factory into a
 * [PublicationServicesHolder].
 *
 * This will ensure that the PDF documents are only cached as long as  the [Publication] object is
 * around.
 */
@ExperimentalReadiumApi
public suspend fun <T : PdfDocument> PdfDocumentFactory<T>.cachedIn(
    holder: PublicationServicesHolder,
): PdfDocumentFactory<T> {
    val namespace = requireNotNull(documentType.qualifiedName)
    val cache = holder.cacheService?.cacheOf(documentType, namespace) ?: return this
    return CachingPdfDocumentFactory(this, cache)
}

private class CachingPdfDocumentFactory<T : PdfDocument>(
    private val factory: PdfDocumentFactory<T>,
    private val cache: Cache<T>,
) : PdfDocumentFactory<T> by factory {

    override suspend fun open(resource: Resource, password: String?): ReadTry<T> {
        val key = resource.sourceUrl?.toString() ?: return factory.open(resource, password)
        return cache.transaction {
            getOrTryPut(key) {
                factory.open(resource, password)
            }
        }
    }
}

/**
 * Represents a PDF document.
 */
public interface PdfDocument : Closeable {

    /**
     * Permanent identifier based on the contents of the file at the time it was originally
     * created.
     */
    public val identifier: String? get() = null

    /**
     * Number of pages in the document.
     */
    public val pageCount: Int

    /**
     * Default reading progression of the document.
     */
    public val readingProgression: ReadingProgression? get() = null

    /**
     * The first page rendered as a cover.
     */
    public suspend fun cover(context: Context): Bitmap? = null

    // Values extracted from the document information dictionary, defined in PDF specification.

    /**
     * The document's title.
     */
    public val title: String? get() = null

    /**
     * The name of the person who created the document.
     */
    public val author: String? get() = null

    /**
     * The subject of the document.
     */
    public val subject: String? get() = null

    /**
     * Keywords associated with the document.
     */
    public val keywords: List<String> get() = emptyList()

    /**
     * Outline to build the table of contents.
     */
    public val outline: List<OutlineNode> get() = emptyList()

    public data class OutlineNode(
        val title: String?,
        val pageNumber: Int?, // Starts from 1.
        val children: List<OutlineNode>,
    )

    // To allow extensions on the Companion object.
    public companion object
}

/**
 * Converts a PDF outline to [Link] objects.
 *
 * @param documentHref HREF of the PDF document in the [Publication] to which the links are
 *        relative to.
 */
@ExperimentalReadiumApi
public fun List<PdfDocument.OutlineNode>.toLinks(documentHref: Url): List<Link> =
    map { it.toLink(documentHref) }

@ExperimentalReadiumApi
public fun PdfDocument.OutlineNode.toLink(documentHref: Url): Link =
    Link(
        href = documentHref.resolve(Url("#page=$pageNumber")!!),
        mediaType = MediaType.PDF,
        title = title,
        children = children.toLinks(documentHref)
    )
