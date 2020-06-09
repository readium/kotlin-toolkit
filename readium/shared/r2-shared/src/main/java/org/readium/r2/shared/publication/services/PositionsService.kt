/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import org.json.JSONObject
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.extensions.toJsonOrNull
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.toJSON

private val positionsLink = Link(
    href= "/~readium/positions",
    type = "application/vnd.readium.position-list+json"
)

/**
 * Provides a list of discrete locations in the publication, no matter what the original format is.
 */
interface PositionsService : Publication.Service {

    /**
     * List of all the positions in the publication, grouped by the resource reading order index.
     */
    val positionsByReadingOrder: List<List<Locator>>

    /**
     * List of all the positions in the publication.
     */
    val positions: List<Locator> get() = positionsByReadingOrder.flatten()

    override val links get() = listOf(positionsLink)

    override fun get(link: Link): Resource? =
        if (link.href != positionsLink.href)
            null
        else
            positions.let {
                StringResource(positionsLink) {
                    JSONObject().apply {
                        put("total", it.size)
                        put("positions", it.toJSON())
                    }.toString()
                }
            }

}

private val Publication.positionsFromManifest: List<Locator> get() =
    links.firstWithMediaType(positionsLink.mediaType!!)
        ?.let { get(it) }
        ?.readAsString()
        ?.successOrNull()
        ?.toJsonOrNull()
        ?.optJSONArray("positions")
        ?.mapNotNull { Locator.fromJSON(it as? JSONObject) }
        .orEmpty()

/**
 * List of all the positions in the publication, grouped by the resource reading order index.
 */
val Publication.positionsByReadingOrder: List<List<Locator>> get() {
    findService(PositionsService::class.java)?.let {
        return it.positionsByReadingOrder
    }

    val locators = positionsFromManifest.groupBy(Locator::href)
    return readingOrder.map { locators[it.href].orEmpty() }
}

/**
 * List of all the positions in the publication.
 */
val Publication.positions: List<Locator> get() {
    return findService(PositionsService::class.java)?.positions
        ?: positionsFromManifest
}

/**
 * List of all the positions in each resource, indexed by their href.
 */
@Deprecated("Use [positionsByReadingOrder] instead", ReplaceWith("positionsByReadingOrder"))
val Publication.positionsByResource: Map<String, List<Locator>>
    get() = positions.groupBy { it.href }


/** Factory to build a [PositionsService] */
var Publication.ServicesBuilder.positionsServiceFactory: ServiceFactory?
    get() = get(PositionsService::class)
    set(value) = set(PositionsService::class, value)

/**
 * Simple [PositionsService] for a [Publication] which generates one position per [readingOrder]
 * resource.
 *
 * @param fallbackMediaType Media type that will be used as a fallback if the Link doesn't specify
 *        any.
 */
class PerResourcePositionsService(
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

        fun createFactory(fallbackMediaType: String): (Publication.Service.Context) -> PerResourcePositionsService = {
            PerResourcePositionsService(
                readingOrder = it.manifest.readingOrder,
                fallbackMediaType = fallbackMediaType
            )
        }

    }

}