/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content.iterators

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.util.Either

/**
 * Creates a [Content.Iterator] instance for the [Resource], starting from the given [Locator].
 *
 * Returns null if the resource media type is not supported.
 */
@ExperimentalReadiumApi
typealias ResourceContentIteratorFactory =
    suspend (resource: Resource, locator: Locator) -> Content.Iterator?

/**
 * A composite [Content.Iterator] which iterates through a whole [publication] and delegates the
 * iteration inside a given resource to media type-specific iterators.
 *
 * @param publication The [Publication] which will be iterated through.
 * @param startLocator Starting [Locator] in the publication.
 * @param resourceContentIteratorFactories List of [ResourceContentIteratorFactory] which will be
 * used to create the iterator for each resource. The factories are tried in order until there's a
 * match.
 */
@ExperimentalReadiumApi
class PublicationContentIterator(
    private val publication: Publication,
    private val startLocator: Locator?,
    private val resourceContentIteratorFactories: List<ResourceContentIteratorFactory>
) : Content.Iterator {

    /**
     * [Content.Iterator] for a resource, associated with its [index] in the reading order.
     */
    private data class IndexedIterator(
        val index: Int,
        val iterator: Content.Iterator
    )

    /**
     * [Content.Element] loaded with [hasPrevious] or [hasNext], associated with the move direction.
     */
    private data class ElementInDirection(
        val element: Content.Element,
        val direction: Direction
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
            ?: throw IllegalStateException("Called previous() without a successful call to hasPrevious() first")

    override suspend fun hasNext(): Boolean {
        currentElement = nextIn(Direction.Forward)
        return currentElement != null
    }

    override fun next(): Content.Element =
        currentElement
            ?.takeIf { it.direction == Direction.Forward }?.element
            ?: throw IllegalStateException("Called next() without a successful call to hasNext() first")

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
     * Returns the [Content.Iterator] for the current [Resource] in the reading order.
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
            startLocator?.let { publication.readingOrder.indexOfFirstWithHref(it.href) }
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
        if (!publication.readingOrder.indices.contains(index)) {
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
        val link = publication.readingOrder[index]
        val locator = location.toLocator(link) ?: return null
        val resource = publication.get(link)

        return resourceContentIteratorFactories
            .firstNotNullOfOrNull { factory ->
                factory(resource, locator)
            }
            ?.let { IndexedIterator(index, it) }
    }

    private enum class Direction(val delta: Int) {
        Forward(+1), Backward(-1)
    }

    private suspend fun IndexedIterator.nextContentIn(direction: Direction): Content.Element? =
        when (direction) {
            Direction.Forward -> iterator.nextOrNull()
            Direction.Backward -> iterator.previousOrNull()
        }

    private fun LocatorOrProgression.toLocator(link: Link): Locator? =
        left
            ?: publication.locatorFromLink(link)?.copyWithLocations(progression = right)
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
