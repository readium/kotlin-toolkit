/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.epub.layoutOf
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.streamer.container.Container
import kotlin.math.ceil

/**
 * Creates the [positionList] for an EPUB [Publication] from its [readingOrder] and [container].
 *
 * The [presentation] is used to apply different calculation strategy if the resource has a
 * reflowable or fixed layout.
 *
 * https://github.com/readium/architecture/blob/master/models/locators/best-practices/format.md#epub
 * https://github.com/readium/architecture/issues/101
 *
 * @param reflowablePositionLength Length in bytes of a position in a reflowable resource. This is
 *        used to split a single reflowable resource into several positions.
 */
internal class EpubPositionListFactory(
    private val container: Container,
    private val readingOrder: List<Link>,
    private val presentation: Presentation,
    private val reflowablePositionLength: Long
) : Publication.PositionListFactory {

    override fun create(): List<Locator> {
        var lastPositionOfPreviousResource = 0
        var positionList = readingOrder.flatMap { link ->
            val positions =
                if (presentation.layoutOf(link) == EpubLayout.FIXED) {
                    createFixed(link, lastPositionOfPreviousResource)
                } else {
                    createReflowable(link, lastPositionOfPreviousResource, container)
                }

            positions.lastOrNull()?.locations?.position?.let {
                lastPositionOfPreviousResource = it
            }

            positions
        }

        // Calculates [totalProgression].
        val totalPageCount = positionList.size
        positionList = positionList.map { locator ->
            val position = locator.locations.position
            if (position == null) {
                locator
            } else {
                locator.copyWithLocations(
                    totalProgression = (position - 1) / totalPageCount.toDouble()
                )
            }
        }

        return positionList
    }

    private fun createFixed(link: Link, startPosition: Int) = listOf(
        createLocator(link,
            progression = 0.0,
            position = startPosition + 1
        )
    )

    private fun createReflowable(link: Link, startPosition: Int, container: Container): List<Locator> {
        // If the resource is encrypted, we use the `originalLength` declared in `encryption.xml`
        // instead of the ZIP entry length.
        val length = link.properties.encryption?.originalLength
            ?: container.dataLength(link.href)

        val pageCount = ceil(length / reflowablePositionLength.toDouble()).toInt()
            .coerceAtLeast(1)

        return (1..pageCount).map { position ->
            createLocator(link,
                progression = (position - 1) / pageCount.toDouble(),
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
