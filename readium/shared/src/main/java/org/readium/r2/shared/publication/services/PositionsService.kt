/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.extensions.toJsonOrNull
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.publication.firstWithMediaType
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.StringResource
import org.readium.r2.shared.resource.readAsString
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

private val positionsMediaType =
    MediaType("application/vnd.readium.position-list+json")!!

private val positionsLink = Link(
    href = Url("/~readium/positions")!!,
    mediaType = positionsMediaType
)

/**
 * Provides a list of discrete locations in the publication, no matter what the original format is.
 */
public interface PositionsService : Publication.Service {

    /**
     * Returns the list of all the positions in the publication, grouped by the resource reading order index.
     */
    public suspend fun positionsByReadingOrder(): List<List<Locator>>

    /**
     * Returns the list of all the positions in the publication.
     */
    public suspend fun positions(): List<Locator> = positionsByReadingOrder().flatten()

    override val links: List<Link> get() = listOf(positionsLink)

    override fun get(href: Url): Resource? {
        if (href != positionsLink.url()) {
            return null
        }

        return StringResource(
            mediaType = positionsMediaType
        ) {
            val positions = positions()
            Try.success(
                JSONObject().apply {
                    put("total", positions.size)
                    put("positions", positions.toJSON())
                }.toString()
            )
        }
    }
}

/**
 * Returns the list of all the positions in the publication, grouped by the resource reading order index.
 */
public suspend fun PublicationServicesHolder.positionsByReadingOrder(): List<List<Locator>> {
    checkNotNull(findService(PositionsService::class)) { "No position service found." }
        .let { return it.positionsByReadingOrder() }
}

/**
 * Returns the list of all the positions in the publication.
 */
public suspend fun PublicationServicesHolder.positions(): List<Locator> {
    checkNotNull(findService(PositionsService::class)) { "No position service found." }
        .let { return it.positions() }
}

/**
 * List of all the positions in each resource, indexed by their href.
 */
@Deprecated(
    "Use [positionsByReadingOrder] instead",
    ReplaceWith("positionsByReadingOrder"),
    level = DeprecationLevel.ERROR
)
public val Publication.positionsByResource: Map<Url, List<Locator>>
    get() = runBlocking { positions().groupBy { it.href } }

/** Factory to build a [PositionsService] */
public var Publication.ServicesBuilder.positionsServiceFactory: ServiceFactory?
    get() = get(PositionsService::class)
    set(value) = set(PositionsService::class, value)

/**
 * Simple [PositionsService] for a [Publication] which generates one position per [readingOrder]
 * resource.
 *
 * @param fallbackMediaType Media type that will be used as a fallback if the Link doesn't specify
 *        any.
 */
public class PerResourcePositionsService(
    private val readingOrder: List<Link>,
    private val fallbackMediaType: MediaType
) : PositionsService {

    override suspend fun positionsByReadingOrder(): List<List<Locator>> {
        val pageCount = readingOrder.size

        return readingOrder.mapIndexed { index, link ->
            listOf(
                Locator(
                    href = link.url(),
                    mediaType = link.mediaType ?: fallbackMediaType,
                    title = link.title,
                    locations = Locator.Locations(
                        position = index + 1,
                        totalProgression = index.toDouble() / pageCount.toDouble()
                    )
                )
            )
        }
    }

    public companion object {

        public fun createFactory(fallbackMediaType: MediaType): (Publication.Service.Context) -> PerResourcePositionsService = {
            PerResourcePositionsService(
                readingOrder = it.manifest.readingOrder,
                fallbackMediaType = fallbackMediaType
            )
        }
    }
}

internal class WebPositionsService(
    private val manifest: Manifest
) : PositionsService {

    private lateinit var _positions: List<Locator>

    override val links: List<Link> =
        listOfNotNull(
            manifest.links.firstWithMediaType(positionsMediaType)
        )

    override suspend fun positions(): List<Locator> {
        if (!::_positions.isInitialized) {
            _positions = computePositions()
        }

        return _positions
    }

    override suspend fun positionsByReadingOrder(): List<List<Locator>> {
        val locators = positions().groupBy(Locator::href)
        return manifest.readingOrder.map { locators[it.url()].orEmpty() }
    }

    private suspend fun computePositions(): List<Locator> =
        links.firstOrNull()
            ?.let { get(it.url()) }
            ?.readAsString()
            ?.getOrNull()
            ?.toJsonOrNull()
            ?.optJSONArray("positions")
            ?.mapNotNull { Locator.fromJSON(it as? JSONObject) }
            .orEmpty()

    companion object {

        fun createFactory(): (Publication.Service.Context) -> WebPositionsService = {
            WebPositionsService(it.manifest)
        }
    }
}
