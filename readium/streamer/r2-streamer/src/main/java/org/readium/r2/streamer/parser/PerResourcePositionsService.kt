/*
 * Module: r2-streamer-kotlin
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
import org.readium.r2.shared.publication.services.PositionsService


/**
 * Positions Service for a [Publication] that will have one position per [readingOrder]
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
internal class PerResourcePositionsService(
    private val readingOrder: List<Link>,
    private val fallbackMediaType: String
) : PositionsService {

    override val positionsByReadingOrder: List<List<Locator>> by lazy {
        val pageCount = readingOrder.size

        readingOrder.mapIndexed { index, link ->
            listOf(Locator(
                href = link.href,
                type = link.type ?: fallbackMediaType,
                title = link.title,
                locations = Locator.Locations(
                    position = index + 1,
                    totalProgression = index.toDouble() / pageCount.toDouble()
                )
            ))
        }
    }

    companion object {

        fun createFactory(fallbackMediaType: String = ""): (Publication.Service.Context) -> PerResourcePositionsService = {
            PerResourcePositionsService(
                readingOrder = it.manifest.readingOrder,
                fallbackMediaType = fallbackMediaType
            )
        }

    }

}

