/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.publication.services.content.iterators.ContentIterator
import org.readium.r2.shared.publication.services.content.iterators.PublicationContentIterator
import org.readium.r2.shared.publication.services.content.iterators.ResourceContentIteratorFactory
import org.readium.r2.shared.util.Ref

/**
 * Provides [ContentIterator] instances to crawl the content of a [Publication].
 */
@ExperimentalReadiumApi
interface ContentIterationService : Publication.Service {
    /**
     * Creates a [ContentIterator] starting from the given [start] location.
     *
     * Returns null if no iterator can be created, for example because no resources are iterable.
     */
    suspend fun iterator(start: Locator?): ContentIterator?
}

/**
 * Returns whether this [Publication] can be iterated on.
 */
@ExperimentalReadiumApi
val Publication.isContentIterable: Boolean
    get() = contentIterationService != null

/**
 * Creates a [ContentIterator] starting from the given location.
 */
@ExperimentalReadiumApi
suspend fun Publication.contentIterator(start: Locator?): ContentIterator? =
    contentIterationService?.iterator(start)

@ExperimentalReadiumApi
private val Publication.contentIterationService: ContentIterationService?
    get() = findService(ContentIterationService::class)

/** Factory to build a [ContentIterationService] */
@ExperimentalReadiumApi
var Publication.ServicesBuilder.contentIterationServiceFactory: ServiceFactory?
    get() = get(ContentIterationService::class)
    set(value) = set(ContentIterationService::class, value)

/**
 * This [ContentIterationService] takes a list of [ResourceContentIteratorFactory] and returns
 * instances of [PublicationContentIterator].
 */
@ExperimentalReadiumApi
class DefaultContentIterationService(
    private val publication: Ref<Publication>,
    private val resourceContentIteratorFactories: List<ResourceContentIteratorFactory>
) : ContentIterationService {

    companion object {
        fun createFactory(
            resourceContentIteratorFactories: List<ResourceContentIteratorFactory>
        ): (Publication.Service.Context) -> DefaultContentIterationService = { context ->
            DefaultContentIterationService(context.publication, resourceContentIteratorFactories)
        }
    }

    override suspend fun iterator(start: Locator?): ContentIterator? {
        val publication = publication() ?: return null

        return PublicationContentIterator(
            publication = publication,
            startLocator = start,
            resourceContentIteratorFactories = resourceContentIteratorFactories
        )
    }
}
