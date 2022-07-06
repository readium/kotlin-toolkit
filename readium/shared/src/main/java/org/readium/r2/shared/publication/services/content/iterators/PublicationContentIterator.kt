/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content.iterators

import kotlinx.coroutines.CompletableDeferred
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.content.Content

/**
 * Creates a [ContentIterator] instance for the [Resource], starting from the given [Locator].
 *
 * Returns null if the resource media type is not supported.
 */
@ExperimentalReadiumApi
typealias ResourceContentIteratorFactory =
    suspend (resource: Resource, locator: Locator) -> ContentIterator?

/**
 * A composite [ContentIterator] which iterates through a whole [publication] and delegates the
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
    private var startLocator: Locator?,
    private val resourceContentIteratorFactories: List<ResourceContentIteratorFactory>
) : ContentIterator {

    /**
     * Current index in the reading order.
     */
    private var currentIndex: Int =
        startLocator?.let { publication.readingOrder.indexOfFirstWithHref(it.href) } ?: 0

    /**
     * [ContentIterator] for the current [Resource] in the reading order.
     */
    private var currentIterator: ContentIterator? = null

    override suspend fun close() {
        currentIterator?.close()
        currentIterator = null
    }

    override suspend fun previous(): Content? =
        nextIn(Direction.Backward)

    override suspend fun next(): Content? =
        nextIn(Direction.Forward)

    private suspend fun nextIn(direction: Direction): Content? {
        val iterator = currentIterator
            ?: initialIterator()
            ?: nextIteratorIn(direction)
            ?: return null

        val content = iterator.nextIn(direction)
        if (content == null) {
            currentIterator = null
            return nextIn(direction)
        }
        return content
    }

    private var isInitialized = false

    private suspend fun initialIterator(): ContentIterator? {
        if (isInitialized) return null
        isInitialized = true

        val link = publication.readingOrder.getOrNull(currentIndex) ?: return null
        currentIterator = loadIteratorAt(link)
        return currentIterator
    }

    private suspend fun nextIteratorIn(direction: Direction): ContentIterator? {
        val nextIterator = loadIteratorIn(direction, fromIndex = currentIndex)
            ?: return null

        currentIndex = nextIterator.index
        currentIterator = nextIterator.iterator
        return currentIterator
    }

    /**
     * [ContentIterator] for a resource, associated with its [index] in the reading order.
     */
    private data class IndexedIterator(
        val index: Int,
        val iterator: ContentIterator
    )

    private suspend fun loadIteratorIn(direction: Direction, fromIndex: Int): IndexedIterator? {
        val index = fromIndex + direction.delta
        val link = publication.readingOrder.getOrNull(index) ?: return null
        var locator = publication.locatorFromLink(link) ?: return null

        if (direction == Direction.Backward) {
            locator = locator.copyWithLocations(progression = 1.0)
        }

        val iterator = loadIteratorAt(link, locator)
            ?: return loadIteratorIn(direction, fromIndex = index)

        return IndexedIterator(index, iterator)
    }

    private suspend fun loadIteratorAt(link: Link, locator: Locator? = null): ContentIterator? {
        val loc = locator ?: publication.locatorFromLink(link) ?: return null
        val resource = publication.get(link)
        return resourceContentIteratorFactories
            .firstNotNullOfOrNull { factory ->
                factory(resource, loc)
            }
    }

    private enum class Direction(val delta: Int) {
        Forward(+1), Backward(-1)
    }

    private suspend fun ContentIterator.nextIn(direction: Direction): Content? =
        when (direction) {
            Direction.Forward -> next()
            Direction.Backward -> previous()
        }
}
