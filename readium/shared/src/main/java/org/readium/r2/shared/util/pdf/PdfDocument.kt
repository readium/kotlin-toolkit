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
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.publication.Link
import java.io.File

@PdfSupport
interface PdfDocumentFactory {

    /** Opens a PDF from a [file]. */
    suspend fun open(file: File, password: String?): PdfDocument

    /** Opens a PDF from a [Fetcher]'s resource. */
    suspend fun open(resource: Resource, password: String?): PdfDocument

}

/**
 * Represents a PDF document.
 *
 * This is not used to render a PDF document, only to access its metadata.
 */
@PdfSupport
interface PdfDocument {

    /**
     * Permanent identifier based on the contents of the file at the time it was originally
     * created.
     */
    val identifier: String?

    /**
     * Number of pages in the document.
     */
    val pageCount: Int

    /**
     * The first page rendered as a cover.
     */
    suspend fun cover(context: Context): Bitmap?

    // Values extracted from the document information dictionary, defined in PDF specification.

    /**
     * The document's title.
     */
    val title: String?

    /**
     * The name of the person who created the document.
     */
    val author: String?

    /**
     * The subject of the document.
     */
    val subject: String?

    /**
     * Keywords associated with the document.
     */
    val keywords: List<String>

    /**
     * Outline to build the table of contents.
     */
    val outline: List<OutlineNode>

    data class OutlineNode(
        val title: String?,
        val pageNumber: Int?,  // Starts from 1.
        val children: List<OutlineNode>
    )

    // To allow extensions on the Companion object.
    companion object { }
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
