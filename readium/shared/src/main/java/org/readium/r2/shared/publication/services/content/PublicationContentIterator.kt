/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.util.Try

/**
 * Creates a [ContentIterator] instance for the given [resource].
 *
 * Return null if the resource format is not supported.
 */
typealias ResourceContentIteratorFactory = suspend (resource: Resource, locator: Locator) -> ContentIterator?

class PublicationContentIterator(
    private val publication: Publication,
    private var startLocator: Locator?,
    private val resourceContentIteratorFactories: List<ResourceContentIteratorFactory>
) : ContentIterator {

    private var startIndex: Int? =
        startLocator?.let { publication.readingOrder.indexOfFirstWithHref(it.href) }
            ?: 0

    private var currentIndex: Int = 0
    private var currentIterator: ContentIterator? = null

    override suspend fun close() {
        currentIterator?.close()
        currentIterator = null
    }

    override suspend fun previous(): Content? {
        val iterator = iteratorBy(-1) ?: return null

        val content = iterator.previous()
        if (content == null) {
            currentIterator = null
            return previous()
        }
        return content
    }

    override suspend fun next(): Content? {
        val iterator = iteratorBy(1) ?: return null

        val content = iterator.next()
        if (content == null) {
            currentIterator = null
            return next()
        }
        return content
    }

    private suspend fun iteratorBy(delta: Int): ContentIterator? {
        currentIterator?.let { return it }

        // For the first requested iterator, we don't want to move by the given delta.
        var newDelta = delta
        startIndex?.let {
            currentIndex = it
            startIndex = null
            newDelta = 0
        }

        val nextIterator = loadIteratorBy(newDelta, fromIndex = currentIndex)
            ?: return null

        currentIndex = nextIterator.index
        currentIterator = nextIterator.iterator
        return currentIterator
    }

    private data class IndexedIterator(
        val index: Int,
        val iterator: ContentIterator
    )

    private suspend fun loadIteratorBy(delta: Int, fromIndex: Int): IndexedIterator? {
        val index = fromIndex + delta
        val link = publication.readingOrder.getOrNull(index) ?: return null
        var locator = publication.locatorFromLink(link) ?: return null

        val start = startLocator
        startLocator = null
        if (start != null) {
            locator = locator.copy(
                locations = start.locations,
                text = start.text
            )
        } else if (delta < 0) {
            locator = locator.copyWithLocations(progression = 1.0)
        }

        val iterator = loadIteratorAt(link, locator)
            ?: return loadIteratorBy(delta, fromIndex = index)

        return IndexedIterator(index, iterator)
    }

    private suspend fun loadIteratorAt(link: Link, locator: Locator): ContentIterator? {
        val resource = publication.get(link)
        return resourceContentIteratorFactories
            .firstNotNullOfOrNull { factory ->
                factory(resource, locator)
            }
    }
}
