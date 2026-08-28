/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content.iterators

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

/**
 * Creates a [Content.Iterator] instance for the [Resource], starting from the
 * given [Locator].
 *
 * Returns null if the resource media type is not supported.
 */
@ExperimentalReadiumApi
public fun interface ResourceContentIteratorFactory {

    /**
     * Creates a [Content.Iterator] instance for the [resource], starting from the given [locator].
     *
     * Returns null if the resource media type is not supported.
     */
    public suspend fun create(
        manifest: Manifest,
        servicesHolder: PublicationServicesHolder,
        readingOrderIndex: Int,
        resource: Resource,
        mediaType: MediaType,
        locator: Locator,
    ): Content.Iterator?
}

/**
 * A composite [Content.Iterator] which iterates through a whole [manifest] and delegates the
 * iteration inside a given resource to media type-specific iterators.
 *
 * @param manifest The [Manifest] of the publication which will be iterated through.
 * @param startLocator Starting [Locator] in the publication.
 * @param resourceContentIteratorFactories List of [ResourceContentIteratorFactory] which will be
 * used to create the iterator for each resource. The factories are tried in order until there's a
 * match.
 */
@ExperimentalReadiumApi
public class PublicationContentIterator(
    private val manifest: Manifest,
    private val container: Container<Resource>,
    private val services: PublicationServicesHolder,
    private val startLocator: Locator?,
    private val resourceContentIteratorFactories: List<ResourceContentIteratorFactory>,
) : Content.Iterator {

    /**
     * [Content.Iterator] for a resource, associated with its [index] in the reading order.
     */
    private data class IndexedIterator(
        val index: Int,
        val iterator: Content.Iterator,
    )

    /**
     * [Content.Element] loaded with [hasPrevious] or [hasNext], associated with the move direction.
     */
    private data class ElementInDirection(
        val element: Content.Element,
        val direction: Direction,
    )

    private var _currentIterator: IndexedIterator? = null
    private var currentElement: ElementInDirection? = null

    override suspend fun hasPrevious(): Boolean {
        currentElement = nextIn(Direction.Backward)
        return currentElement != null
    }

    override fun previous(): Content.Element =
        currentElement
            ?.takeIf { it.direction == Direction.Backward }?.element
            ?: throw IllegalStateException(
                "Called previous() without a successful call to hasPrevious() first"
            )

    override suspend fun hasNext(): Boolean {
        currentElement = nextIn(Direction.Forward)
        return currentElement != null
    }

    override fun next(): Content.Element =
        currentElement
            ?.takeIf { it.direction == Direction.Forward }?.element
            ?: throw IllegalStateException(
                "Called next() without a successful call to hasNext() first"
            )

    private suspend fun nextIn(direction: Direction): ElementInDirection? {
        val iterator = currentIterator() ?: return null

        val content = iterator.nextContentIn(direction)
        if (content == null) {
            _currentIterator = nextIteratorIn(direction, fromIndex = iterator.index)
                ?: return null
            return nextIn(direction)
        }
        return ElementInDirection(content, direction)
    }

    /**
     * Returns the [Content.Iterator] for the current resource in the reading order.
     */
    private suspend fun currentIterator(): IndexedIterator? {
        if (_currentIterator == null) {
            _currentIterator = initialIterator()
        }
        return _currentIterator
    }

    /**
     * Returns the first iterator starting at [startLocator] or the beginning of the publication.
     */
    private suspend fun initialIterator(): IndexedIterator? {
        val index: Int =
            startLocator?.let { manifest.readingOrder.indexOfFirstWithHref(it.href) }
                ?: 0

        val locations = startLocator.orProgression(0.0)

        return loadIteratorAt(index, locations)
            ?: nextIteratorIn(Direction.Forward, fromIndex = index)
    }

    /**
     * Returns the next resource iterator in the given [direction], starting from [fromIndex].
     */
    private suspend fun nextIteratorIn(direction: Direction, fromIndex: Int): IndexedIterator? {
        val index = fromIndex + direction.delta
        if (!manifest.readingOrder.indices.contains(index)) {
            return null
        }

        val progression = when (direction) {
            Direction.Forward -> 0.0
            Direction.Backward -> 1.0
        }

        return loadIteratorAt(index, location = Either.Right(progression))
            ?: nextIteratorIn(direction, fromIndex = index)
    }

    /**
     * Loads the iterator at the given [index] in the reading order.
     *
     * The [location] will be used to compute the starting [Locator] for the iterator.
     */
    private suspend fun loadIteratorAt(index: Int, location: LocatorOrProgression): IndexedIterator? {
        val link = manifest.readingOrder[index]
        val locator = location.toLocator(link) ?: return null
        val resource = container[link.url()] ?: return null
        val mediaType = link.mediaType ?: return null

        return resourceContentIteratorFactories
            .firstNotNullOfOrNull { factory ->
                factory.create(manifest, services, index, resource, mediaType, locator)
            }
            ?.let { IndexedIterator(index, it) }
    }

    private enum class Direction(val delta: Int) {
        Forward(+1),
        Backward(-1),
    }

    private suspend fun IndexedIterator.nextContentIn(direction: Direction): Content.Element? =
        when (direction) {
            Direction.Forward -> iterator.nextOrNull()
            Direction.Backward -> iterator.previousOrNull()
        }

    private fun LocatorOrProgression.toLocator(link: Link): Locator? =
        left
            ?: manifest.locatorFromLink(link)?.copyWithLocations(progression = right)
}

/**
 * Represents either an full [Locator], or a progression percentage in a resource.
 */
private typealias LocatorOrProgression = Either<Locator, Double>

/**
 * Returns this locator if not null, or the given [progression] as a fallback.
 */
private fun Locator?.orProgression(progression: Double): LocatorOrProgression =
    Either(this ?: progression)
