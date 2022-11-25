/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.readium

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.pdf.cachedIn
import timber.log.Timber

/**
 * Creates the [positions] for an LCP protected PDF [Publication] from its [readingOrder] and
 * [fetcher].
 */
@OptIn(PdfSupport::class, ExperimentalReadiumApi::class)
internal class LcpdfPositionsService(
    private val pdfFactory: PdfDocumentFactory<*>,
    private val context: Publication.Service.Context,
) : PositionsService {

    override suspend fun positionsByReadingOrder(): List<List<Locator>> {
        if (!::_positions.isInitialized)
            _positions = computePositions()

        return _positions
    }

    private lateinit var _positions: List<List<Locator>>

    private suspend fun computePositions(): List<List<Locator>> {
        // Calculates the page count of each resource from the reading order.
        val resources: List<Pair<Int, Link>> = context.manifest.readingOrder.map { link ->
            val pageCount = openPdfAt(link)?.pageCount ?: 0
            Pair(pageCount, link)
        }

        val totalPageCount = resources.sumOf { it.first }
        if (totalPageCount <= 0) {
            return emptyList()
        }

        var lastPositionOfPreviousResource = 0
        return resources.map { (pageCount, link) ->
            val positions = createPositionsOf(link, pageCount = pageCount, totalPageCount = totalPageCount, startPosition = lastPositionOfPreviousResource)
            lastPositionOfPreviousResource += pageCount
            positions
        }
    }

    private fun createPositionsOf(
        link: Link,
        pageCount: Int,
        totalPageCount: Int,
        startPosition: Int
    ): List<Locator> {
        if (pageCount <= 0 || totalPageCount <= 0) {
            return emptyList()
        }

        // FIXME: Use the [tableOfContents] to generate the titles
        return (1..pageCount).map { position ->
            val progression = (position - 1) / pageCount.toDouble()
            val totalProgression = (startPosition + position - 1) / totalPageCount.toDouble()
            Locator(
                href = link.href,
                type = link.type ?: MediaType.PDF.toString(),
                locations = Locator.Locations(
                    fragments = listOf("page=$position"),
                    progression = progression,
                    totalProgression = totalProgression,
                    position = startPosition + position
                )
            )
        }
    }

    private suspend fun openPdfAt(link: Link): PdfDocument? =
        try {
            pdfFactory
                .cachedIn(context.services)
                .open(context.fetcher.get(link), password = null)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    companion object {

        fun create(pdfFactory: PdfDocumentFactory<*>): (Publication.Service.Context) -> LcpdfPositionsService = { serviceContext ->
            LcpdfPositionsService(
                pdfFactory = pdfFactory,
                context = serviceContext
            )
        }
    }
}
