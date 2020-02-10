/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.cbz

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Creates the [positionList] for a CBZ [Publication] from its [readingOrder].
 *
 * https://github.com/readium/architecture/blob/master/models/locators/best-practices/format.md#comics
 * https://github.com/readium/architecture/issues/101
 */
internal class CbzPositionListFactory(private val readingOrder: List<Link>) : Publication.PositionListFactory {

    override fun create(): List<Locator> {
        val pageCount = readingOrder.size

        return readingOrder.mapIndexed { index, link ->
            Locator(
                href = link.href,
                type = link.type ?: "image/*",
                title = link.title,
                locations = Locator.Locations(
                    position = index + 1,
                    totalProgression = index.toDouble() / pageCount.toDouble()
                )
            )
        }
    }

}
