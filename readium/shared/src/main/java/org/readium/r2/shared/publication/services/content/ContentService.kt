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
import org.readium.r2.shared.publication.services.content.iterators.PublicationContentIterator
import org.readium.r2.shared.publication.services.content.iterators.ResourceContentIteratorFactory
import org.readium.r2.shared.util.Ref

/**
 * Provides a way to extract the raw [Content] of a [Publication].
 */
@ExperimentalReadiumApi
interface ContentService : Publication.Service {
    /**
     * Creates a [Content] starting from the given [start] location.
     *
     * The implementation must be fast and non-blocking. Do the actual extraction inside the
     * [Content] implementation.
     */
    fun content(start: Locator?): Content
}

/**
 * Creates a [Content] starting from the given [start] location, or the beginning of the
 * publication when missing.
 */
@ExperimentalReadiumApi
fun Publication.content(start: Locator? = null): Content? =
    contentService?.content(start)

@ExperimentalReadiumApi
private val Publication.contentService: ContentService?
    get() = findService(ContentService::class)

/** Factory to build a [ContentService] */
@ExperimentalReadiumApi
var Publication.ServicesBuilder.contentServiceFactory: ServiceFactory?
    get() = get(ContentService::class)
    set(value) = set(ContentService::class, value)

/**
 * Default implementation of [DefaultContentService], delegating the content parsing to
 * [ResourceContentIteratorFactory].
 */
@ExperimentalReadiumApi
class DefaultContentService(
    private val publication: Ref<Publication>,
    private val resourceContentIteratorFactories: List<ResourceContentIteratorFactory>
) : ContentService {

    companion object {
        fun createFactory(
            resourceContentIteratorFactories: List<ResourceContentIteratorFactory>
        ): (Publication.Service.Context) -> DefaultContentService = { context ->
            DefaultContentService(context.publication, resourceContentIteratorFactories)
        }
    }

    override fun content(start: Locator?): Content {
        val publication = publication() ?: throw IllegalStateException("No Publication object")
        return ContentImpl(publication, start)
    }

    private inner class ContentImpl(
        val publication: Publication,
        val start: Locator?,
    ) : Content {
        override fun iterator(): Content.Iterator =
            PublicationContentIterator(
                publication = publication,
                startLocator = start,
                resourceContentIteratorFactories = resourceContentIteratorFactories
            )
    }
}
