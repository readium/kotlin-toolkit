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
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.pdf.cachedIn
import org.readium.r2.shared.util.toDebugDescription
import timber.log.Timber

/**
 * Creates the [positions] for an LCP protected PDF [Publication] from its reading order and
 * container.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class LcpdfPositionsService(
    private val pdfFactory: PdfDocumentFactory<*>,
    private val context: Publication.Service.Context,
) : PositionsService {

    override suspend fun positionsByReadingOrder(): List<List<Locator>> {
        if (!::_positions.isInitialized) {
            _positions = computePositions()
        }

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
            val positions = createPositionsOf(
                link,
                pageCount = pageCount,
                totalPageCount = totalPageCount,
                startPosition = lastPositionOfPreviousResource
            )
            lastPositionOfPreviousResource += pageCount
            positions
        }
    }

    private fun createPositionsOf(
        link: Link,
        pageCount: Int,
        totalPageCount: Int,
        startPosition: Int,
    ): List<Locator> {
        if (pageCount <= 0 || totalPageCount <= 0) {
            return emptyList()
        }

        val href = link.url()

        // FIXME: Use the [tableOfContents] to generate the titles
        return (1..pageCount).map { position ->
            val progression = (position - 1) / pageCount.toDouble()
            val totalProgression = (startPosition + position - 1) / totalPageCount.toDouble()
            Locator(
                href = href,
                mediaType = link.mediaType ?: MediaType.PDF,
                locations = Locator.Locations(
                    fragments = listOf("page=$position"),
                    progression = progression,
                    totalProgression = totalProgression,
                    position = startPosition + position
                )
            )
        }
    }

    private suspend fun openPdfAt(link: Link): PdfDocument? {
        val resource = context.container.get(link.url())
            ?: return null

        return pdfFactory
            .cachedIn(context.services)
            .open(resource, password = null)
            .getOrElse {
                Timber.e(it.toDebugDescription())
                null
            }
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
