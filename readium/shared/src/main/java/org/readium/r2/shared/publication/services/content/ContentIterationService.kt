/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.util.Ref

interface ContentIterationService : Publication.Service {
    suspend fun iterator(start: Locator?): ContentIterator?
}

val Publication.isContentIterable: Boolean
    get() = contentIterationService != null

suspend fun Publication.contentIterator(start: Locator?): ContentIterator? =
    contentIterationService?.iterator(start)

private val Publication.contentIterationService: ContentIterationService?
    get() = findService(ContentIterationService::class)

/** Factory to build a [ContentIterationService] */
var Publication.ServicesBuilder.contentIterationServiceFactory: ServiceFactory?
    get() = get(ContentIterationService::class)
    set(value) = set(ContentIterationService::class, value)

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
