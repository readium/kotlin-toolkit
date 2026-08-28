/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import org.readium.r2.shared.publication.*
import timber.log.Timber

/**
 * Locates the destination of various sources (e.g. locators, progression, etc.) in the
 * publication.
 *
 * This service can be used to implement a variety of features, such as:
 *   - Jumping to a given position or total progression, by converting it first to a [Locator].
 *   - Converting a [Locator] which was created from an alternate manifest with a different reading
 *     order. For example, when downloading a streamed manifest or offloading a package.
 */
public interface LocatorService : Publication.Service {

    /** Locates the target of the given [locator]. */
    public suspend fun locate(locator: Locator): Locator?

    /** Locates the target at the given [totalProgression] relative to the whole publication. */
    public suspend fun locateProgression(totalProgression: Double): Locator?
}

/** Locates the target of the given [locator]. */
public suspend fun Publication.locate(locator: Locator): Locator? =
    findService(LocatorService::class)?.locate(locator)

/** Locates the target at the given progression relative to the whole publication. */
public suspend fun Publication.locateProgression(totalProgression: Double): Locator? =
    findService(LocatorService::class)?.locateProgression(totalProgression)

/** Factory to build a [LocatorService] */
public var Publication.ServicesBuilder.locatorServiceFactory: ServiceFactory?
    get() = get(LocatorService::class)
    set(value) = set(LocatorService::class, value)

public open class DefaultLocatorService(
    public val readingOrder: List<Link>,
    public val positionsByReadingOrder: suspend () -> List<List<Locator>>,
) : LocatorService {

    public constructor(readingOrder: List<Link>, services: PublicationServicesHolder) :
        this(readingOrder, positionsByReadingOrder = {
            services.findService(PositionsService::class)?.positionsByReadingOrder() ?: emptyList()
        })

    override suspend fun locate(locator: Locator): Locator? =
        locator.takeIf { readingOrder.firstWithHref(locator.href) != null }

    override suspend fun locateProgression(totalProgression: Double): Locator? {
        if (totalProgression !in 0.0..1.0) {
            Timber.e("Progression must be between 0.0 and 1.0, received $totalProgression)")
            return null
        }

        val positions = positionsByReadingOrder()
        val (readingOrderIndex, position) = findClosestTo(totalProgression, positions)
            ?: return null

        return position.copyWithLocations(
            progression = resourceProgressionFor(
                totalProgression,
                positions,
                readingOrderIndex = readingOrderIndex
            )
                ?: position.locations.progression,
            totalProgression = totalProgression
        )
    }

    private data class Position(val readingOrderIndex: Int, val locator: Locator)

    /**
     * Finds the [Locator] in the given [positions] which is the closest to the given
     * [totalProgression], without exceeding it.
     */
    private fun findClosestTo(totalProgression: Double, positions: List<List<Locator>>): Position? {
        val lastPosition = findLast(positions) ?: return null
        val lastProgression = lastPosition.item.locations.totalProgression
        if (lastProgression != null && totalProgression >= lastProgression) {
            return Position(lastPosition.x, lastPosition.item)
        }

        fun inBetween(first: Locator, second: Locator): Boolean {
            val prog1 = first.locations.totalProgression ?: return false
            val prog2 = second.locations.totalProgression ?: return false
            return prog1 <= totalProgression && totalProgression < prog2
        }

        val position = findFirstByPair(positions, ::inBetween) ?: return null
        return Position(position.x, position.item)
    }

    /**
     * Computes the progression relative to a reading order resource at the given index, from its
     * [totalProgression] relative to the whole publication.
     */
    private fun resourceProgressionFor(
        totalProgression: Double,
        positions: List<List<Locator>>,
        readingOrderIndex: Int,
    ): Double? {
        val startProgression = positions[readingOrderIndex].firstOrNull()?.locations?.totalProgression ?: return null
        val endProgression = positions.getOrNull(readingOrderIndex + 1)?.firstOrNull()?.locations?.totalProgression ?: 1.0

        return when {
            totalProgression <= startProgression -> 0.0
            totalProgression >= endProgression -> 1.0
            else -> (totalProgression - startProgression) / (endProgression - startProgression)
        }
    }

    /** Holds an item and its position in a two-dimensional array. */
    private data class Match<T>(val x: Int, val y: Int, val item: T)

    /* Finds the first item matching the given condition when paired with its successor. */
    private fun <T> findFirstByPair(items: List<List<T>>, condition: (T, T) -> Boolean): Match<T>? {
        var previous: Match<T>? = null

        items.forEachIndexed { x, section ->
            section.forEachIndexed { y, item ->
                previous?.let { previous ->
                    if (condition(previous.item, item)) {
                        return previous
                    }
                }

                previous = Match(x = x, y = y, item = item)
            }
        }

        return null
    }

    /** Finds the last item in the last non-empty list of [items]. */
    private fun <T> findLast(items: List<List<T>>): Match<T>? {
        var last: Match<T>? = null

        items.forEachIndexed { x, section ->
            section.forEachIndexed { y, item ->
                last = Match(x = x, y = y, item = item)
            }
        }

        return last
    }
}
