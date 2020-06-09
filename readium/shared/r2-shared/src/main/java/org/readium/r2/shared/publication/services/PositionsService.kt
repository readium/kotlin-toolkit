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
            StringResource(positionsLink) {
                JSONObject().apply {
                    put("total", positions.size)
                    put("positions", positions.toJSON())
                }.toString()
            }

}

private fun Publication.positionsFromManifest(): List<Locator>? =
    links.firstWithMediaType(positionsLink.mediaType!!)
        ?.let { get(it) }
        ?.readAsString()
        ?.successOrNull()
        ?.toJsonOrNull()
        ?.optJSONArray("positions")
        ?.mapNotNull { Locator.fromJSON(it as? JSONObject) }

/**
 * List of all the positions in the publication, grouped by the resource reading order index.
 */
val Publication.positionsByReadingOrder: List<List<Locator>> get() {
    findService(PositionsService::class.java)?.let {
        return it.positionsByReadingOrder
    }

    val locators = positionsFromManifest()
        .orEmpty()
        .groupBy(Locator::href)

    return readingOrder.map { locators[it.href].orEmpty() }
}

/**
 * List of all the positions in the publication.
 */
val Publication.positions: List<Locator> get() {
    findService(PositionsService::class.java)?.let {
        return it.positions
    }

    return positionsFromManifest().orEmpty()
}

/**
 * List of all the positions in each resource, indexed by their href.
 */
@Deprecated("Use [positionsByReadingOrder] instead", ReplaceWith("positionsByReadingOrder"))
val Publication.positionsByResource: Map<String, List<Locator>>
    get() = positions.groupBy { it.href }


/** Factory to build a [PositionsService] */
var Publication.ServicesBuilder.positionsServiceFactory: ServiceFactory?
    get() = serviceFactories[PositionsService::class.simpleName]
    set(value) {
        if (value == null)
            serviceFactories.remove(PositionsService::class.simpleName!!)
        else
            serviceFactories[PositionsService::class.simpleName!!] = value
    }
