/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.pdf

import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PositionsService
import timber.log.Timber

/**
 * Creates the [positions] for a PDF [Publication].
 *
 * @param link The [Link] to the PDF document in the [Publication].
 * @param pageCount Total page count in the PDF document.
 * @param tableOfContents Table of contents used to compute the position titles.
 */
internal class PdfPositionsService(
    private val link: Link,
    private val pageCount: Int,
    private val tableOfContents: List<Link>
) : PositionsService {

    override suspend fun positionsByReadingOrder(): List<List<Locator>> = _positions

    private val _positions: List<List<Locator>> by lazy {
        // FIXME: Use the [tableOfContents] to generate the titles
        if (pageCount <= 0) {
            Timber.e("Invalid page count for a PDF document: $pageCount")
            return@lazy listOf(emptyList<Locator>())
        }

        return@lazy listOf((1..pageCount).map { position ->
            val progression = (position - 1) / pageCount.toDouble()
            Locator(
                href = link.href,
                type = link.type ?: MediaType.PDF.toString(),
                locations = Locator.Locations(
                    fragments = listOf("page=$position"),
                    progression = progression,
                    totalProgression = progression,
                    position = position
                )
            )
        })
    }

    companion object {

        fun create(context: Publication.Service.Context): PdfPositionsService? {
            val link = context.manifest.readingOrder.firstOrNull() ?: return null

            return PdfPositionsService(
                link = link,
                pageCount = context.manifest.metadata.numberOfPages ?: 0,
                tableOfContents = context.manifest.tableOfContents
            )
        }

    }

}
