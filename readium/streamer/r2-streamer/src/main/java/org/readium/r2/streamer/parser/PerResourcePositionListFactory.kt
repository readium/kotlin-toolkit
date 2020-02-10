/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Creates the [positionList] for a [Publication] that will have one position per [readingOrder]
 * resource.
 *
 * This is used for CBZ and DiViNa formats.
 *
 * https://github.com/readium/architecture/blob/master/models/locators/best-practices/format.md#comics
 * https://github.com/readium/architecture/issues/101
 *
 * @param fallbackMediaType Media type that will be used as a fallback if the Link doesn't specify
 *        any.
 */
internal class PerResourcePositionListFactory(
    private val readingOrder: List<Link>,
    private val fallbackMediaType: String = ""
) : Publication.PositionListFactory {

    override fun create(): List<Locator> {
        val pageCount = readingOrder.size

        return readingOrder.mapIndexed { index, link ->
            Locator(
                href = link.href,
                type = link.type ?: fallbackMediaType,
                title = link.title,
                locations = Locator.Locations(
                    position = index + 1,
                    totalProgression = index.toDouble() / pageCount.toDouble()
                )
            )
        }
    }

}
