/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.publication.services.content.iterators.PublicationContentIterator
import org.readium.r2.shared.publication.services.content.iterators.ResourceContentIteratorFactory
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.resource.Resource

/**
 * Provides a way to extract the raw [Content] of a [Publication].
 */
@ExperimentalReadiumApi
public interface ContentService : Publication.Service {
    /**
     * Creates a [Content] starting from the given [start] location.
     *
     * The implementation must be fast and non-blocking. Do the actual extraction inside the
     * [Content] implementation.
     */
    public fun content(start: Locator?): Content
}

/**
 * Creates a [Content] starting from the given [start] location, or the beginning of the
 * publication when missing.
 */
@ExperimentalReadiumApi
public fun Publication.content(start: Locator? = null): Content? =
    contentService?.content(start)

@ExperimentalReadiumApi
private val Publication.contentService: ContentService?
    get() = findService(ContentService::class)

/** Factory to build a [ContentService] */
@ExperimentalReadiumApi
public var Publication.ServicesBuilder.contentServiceFactory: ServiceFactory?
    get() = get(ContentService::class)
    set(value) = set(ContentService::class, value)

/**
 * Default implementation of [DefaultContentService], delegating the content parsing to
 * [ResourceContentIteratorFactory].
 */
@ExperimentalReadiumApi
public class DefaultContentService(
    private val manifest: Manifest,
    private val container: Container<Resource>,
    private val services: PublicationServicesHolder,
    private val resourceContentIteratorFactories: List<ResourceContentIteratorFactory>,
) : ContentService {

    public companion object {
        public fun createFactory(
            resourceContentIteratorFactories: List<ResourceContentIteratorFactory>,
        ): (Publication.Service.Context) -> DefaultContentService = { context ->
            DefaultContentService(
                context.manifest,
                context.container,
                context.services,
                resourceContentIteratorFactories
            )
        }
    }

    override fun content(start: Locator?): Content {
        return ContentImpl(manifest, container, services, start)
    }

    private inner class ContentImpl(
        val manifest: Manifest,
        val container: Container<Resource>,
        val services: PublicationServicesHolder,
        val start: Locator?,
    ) : Content {
        override fun iterator(): Content.Iterator =
            PublicationContentIterator(
                manifest = manifest,
                container = container,
                services = services,
                startLocator = start,
                resourceContentIteratorFactories = resourceContentIteratorFactories
            )
    }
}
