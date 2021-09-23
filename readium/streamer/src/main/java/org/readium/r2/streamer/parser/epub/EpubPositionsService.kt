/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.archive.archive
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.epub.layoutOf
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.util.use
import kotlin.math.ceil

/**
 * Positions Service for an EPUB from its [readingOrder] and [fetcher].
 *
 * The [presentation] is used to apply different calculation strategy if the resource has a
 * reflowable or fixed layout.
 *
 * https://github.com/readium/architecture/blob/master/models/locators/best-practices/format.md#epub
 * https://github.com/readium/architecture/issues/101
 */
class EpubPositionsService(
    private val readingOrder: List<Link>,
    private val presentation: Presentation,
    private val fetcher: Fetcher,
    private val reflowableStrategy: ReflowableStrategy
) : PositionsService {

    companion object {

        fun createFactory(reflowableStrategy: ReflowableStrategy = ReflowableStrategy.recommended): (Publication.Service.Context) -> EpubPositionsService =
            { context ->
                EpubPositionsService(
                    readingOrder = context.manifest.readingOrder,
                    presentation = context.manifest.metadata.presentation,
                    fetcher = context.fetcher,
                    reflowableStrategy = reflowableStrategy
                )
            }
    }

    /**
     * Strategy used to calculate the number of positions in a reflowable resource.
     *
     * Note that a fixed-layout resource always has a single position.
     */
    sealed class ReflowableStrategy {
        /** Returns the number of positions in the given [resource] according to the strategy. */
        abstract suspend fun positionCount(resource: Resource): Int

        /**
         * Use the original length of each resource (before compression and encryption) and split it
         * by the given [pageLength].
         */
        data class OriginalLength(val pageLength: Int) : ReflowableStrategy() {
            override suspend fun positionCount(resource: Resource): Int {
                val length = resource.link().properties.encryption?.originalLength
                    ?: resource.length().getOrNull()
                    ?: 0
                return ceil(length.toDouble() / pageLength.toDouble()).toInt()
                    .coerceAtLeast(1)
            }
        }

        /**
         * Use the archive entry length (whether it is compressed or stored) and split it by the
         * given [pageLength].
         */
        data class ArchiveEntryLength(val pageLength: Int) : ReflowableStrategy() {
            override suspend fun positionCount(resource: Resource): Int {
                val length = resource.link().properties.archive?.entryLength
                    ?: resource.length().getOrNull()
                    ?: 0
                return ceil(length.toDouble() / pageLength.toDouble()).toInt()
                    .coerceAtLeast(1)
            }
        }

        companion object {
            /**
             * Recommended historical strategy: archive entry length split by 1024 bytes pages.
             *
             * This strategy is used by Adobe RMSDK as well.
             * See https://github.com/readium/architecture/issues/123
             */
            val recommended = ArchiveEntryLength(pageLength = 1024)
        }
    }

    override suspend fun positionsByReadingOrder(): List<List<Locator>> {
        if (!::_positions.isInitialized)
            _positions = computePositions()

        return _positions
    }

    private lateinit var _positions: List<List<Locator>>

    private suspend fun computePositions(): List<List<Locator>> {
        var lastPositionOfPreviousResource = 0
        var positions = readingOrder.map { link ->
            val positions =
                if (presentation.layoutOf(link) == EpubLayout.FIXED) {
                    createFixed(link, lastPositionOfPreviousResource)
                } else {
                    createReflowable(link, lastPositionOfPreviousResource, fetcher)
                }

            positions.lastOrNull()?.locations?.position?.let {
                lastPositionOfPreviousResource = it
            }

            positions
        }

        // Calculates [totalProgression].
        val totalPageCount = positions.map { it.size }.sum()
        positions = positions.map { item ->
            item.map { locator ->
                val position = locator.locations.position
                if (position == null) {
                    locator
                } else {
                    locator.copyWithLocations(
                        totalProgression = (position - 1) / totalPageCount.toDouble()
                    )
                }
            }
        }

        return positions
    }

    private fun createFixed(link: Link, startPosition: Int) = listOf(
        createLocator(link,
            progression = 0.0,
            position = startPosition + 1
        )
    )

    private suspend fun createReflowable(link: Link, startPosition: Int, fetcher: Fetcher): List<Locator> {
        val positionCount = fetcher.get(link).use { resource ->
            reflowableStrategy.positionCount(resource)
        }

        return (1..positionCount).map { position ->
            createLocator(link,
                progression = (position - 1) / positionCount.toDouble(),
                position = startPosition + position
            )
        }
    }

    private fun createLocator(link: Link, progression: Double, position: Int) = Locator(
        href = link.href,
        type = link.type ?: "text/html",
        title = link.title,
        locations = Locator.Locations(
            progression = progression,
            position = position
        )
    )
}
