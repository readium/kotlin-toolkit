/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import kotlin.math.ceil
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.epub.layoutOf
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.archive
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use

/**
 * Positions Service for an EPUB from its [readingOrder] and [container].
 *
 * The [presentation] is used to apply different calculation strategy if the resource has a
 * reflowable or fixed layout.
 *
 * https://github.com/readium/architecture/blob/master/models/locators/best-practices/format.md#epub
 * https://github.com/readium/architecture/issues/101
 */
public class EpubPositionsService(
    private val readingOrder: List<Link>,
    private val presentation: Presentation,
    private val container: Container<Resource>,
    private val reflowableStrategy: ReflowableStrategy,
) : PositionsService {

    public companion object {

        public fun createFactory(
            reflowableStrategy: ReflowableStrategy = ReflowableStrategy.recommended,
        ): (
            Publication.Service.Context,
        ) -> EpubPositionsService =
            { context ->
                EpubPositionsService(
                    readingOrder = context.manifest.readingOrder,
                    presentation = context.manifest.metadata.presentation,
                    container = context.container,
                    reflowableStrategy = reflowableStrategy
                )
            }
    }

    /**
     * Strategy used to calculate the number of positions in a reflowable resource.
     *
     * Note that a fixed-layout resource always has a single position.
     */
    public sealed class ReflowableStrategy {
        /** Returns the number of positions in the given [resource] according to the strategy. */
        public abstract suspend fun positionCount(link: Link, resource: Resource): Int

        /**
         * Use the original length of each resource (before compression and encryption) and split it
         * by the given [pageLength].
         */
        public data class OriginalLength(val pageLength: Int) : ReflowableStrategy() {
            override suspend fun positionCount(link: Link, resource: Resource): Int {
                val length = link.properties.encryption?.originalLength
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
        public data class ArchiveEntryLength(val pageLength: Int) : ReflowableStrategy() {
            override suspend fun positionCount(link: Link, resource: Resource): Int {
                val length = resource.properties().getOrNull()?.archive?.entryLength
                    ?: resource.length().getOrNull()
                    ?: 0
                return ceil(length.toDouble() / pageLength.toDouble()).toInt()
                    .coerceAtLeast(1)
            }
        }

        public companion object {
            /**
             * Recommended historical strategy: archive entry length split by 1024 bytes pages.
             *
             * This strategy is used by Adobe RMSDK as well.
             * See https://github.com/readium/architecture/issues/123
             */
            public val recommended: ReflowableStrategy = ArchiveEntryLength(pageLength = 1024)
        }
    }

    override suspend fun positionsByReadingOrder(): List<List<Locator>> {
        if (!::_positions.isInitialized) {
            _positions = computePositions()
        }

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
                    container.get(link.url())
                        ?.use { createReflowable(link, lastPositionOfPreviousResource, it) }
                        ?: emptyList()
                }

            positions.lastOrNull()?.locations?.position?.let {
                lastPositionOfPreviousResource = it
            }

            positions
        }

        // Calculates [totalProgression].
        val totalPageCount = positions.sumOf { it.size }
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

    private fun createFixed(link: Link, startPosition: Int): List<Locator> =
        listOf(
            createLocator(
                href = link.url(),
                type = link.mediaType,
                title = link.title,
                progression = 0.0,
                position = startPosition + 1
            )
        )

    private suspend fun createReflowable(link: Link, startPosition: Int, resource: Resource): List<Locator> {
        val href = link.url()

        val positionCount =
            reflowableStrategy.positionCount(link, resource)

        return (1..positionCount).mapNotNull { position ->
            createLocator(
                href = href,
                type = link.mediaType,
                title = link.title,
                progression = (position - 1) / positionCount.toDouble(),
                position = startPosition + position
            )
        }
    }

    private fun createLocator(
        href: Url,
        type: MediaType?,
        title: String?,
        progression: Double,
        position: Int,
    ): Locator =
        Locator(
            href = href,
            mediaType = type ?: MediaType.XHTML,
            title = title,
            locations = Locator.Locations(
                progression = progression,
                position = position
            )
        )
}
