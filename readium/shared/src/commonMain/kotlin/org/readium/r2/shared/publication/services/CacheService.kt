/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import kotlin.reflect.KClass
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.util.cache.Cache

/**
 * Provides publication-bound caches.
 */
@InternalReadiumApi
public interface CacheService : Publication.Service {
    /**
     * Gets the cache for objects of [valueType] in the given [namespace].
     */
    public suspend fun <T : Any> cacheOf(valueType: KClass<T>, namespace: String): Cache<T>
}

@InternalReadiumApi
public val PublicationServicesHolder.cacheService: CacheService?
    get() = findService(CacheService::class)

/** Factory to build a [CacheService]. */
@InternalReadiumApi
public var Publication.ServicesBuilder.cacheServiceFactory: ServiceFactory?
    get() = get(CacheService::class)
    set(value) = set(CacheService::class, value)
