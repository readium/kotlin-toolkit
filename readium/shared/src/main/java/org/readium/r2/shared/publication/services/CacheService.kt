/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import android.content.ComponentCallbacks2
import android.content.Context
import kotlin.reflect.KClass
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.util.MemoryObserver
import org.readium.r2.shared.util.cache.Cache
import org.readium.r2.shared.util.cache.InMemoryCache

/**
 * Provides publication-bound caches.
 */
@InternalReadiumApi
interface CacheService : Publication.Service {
    /**
     * Gets the cache for objects of [valueType] in the given [namespace].
     */
    suspend fun <T : Any> cacheOf(valueType: KClass<T>, namespace: String): Cache<T>
}

@InternalReadiumApi
val PublicationServicesHolder.cacheService: CacheService?
    get() = findService(CacheService::class)

/** Factory to build a [CacheService]. */
@InternalReadiumApi
var Publication.ServicesBuilder.cacheServiceFactory: ServiceFactory?
    get() = get(CacheService::class)
    set(value) = set(CacheService::class, value)

/**
 * A basic [CacheService] implementation keeping the cached objects in memory.
 */
@InternalReadiumApi
class InMemoryCacheService(context: Context?) : CacheService, MemoryObserver {

    companion object {
        fun createFactory(context: Context?): (Publication.Service.Context) -> InMemoryCacheService = { _ ->
            InMemoryCacheService(context)
        }
    }

    private val context = context?.applicationContext
    private val caches = mutableMapOf<String, Cache<*>>()
    private val mutex = Mutex()
    private val componentCallbacks: ComponentCallbacks2 = MemoryObserver.asComponentCallbacks2(this)

    init {
        context?.registerComponentCallbacks(componentCallbacks)
    }

    override suspend fun <T : Any> cacheOf(valueType: KClass<T>, namespace: String): Cache<T> =
        mutex.withLock {
            val valueTypeName = requireNotNull(valueType.qualifiedName)
            val cache = caches.getOrPut("$namespace.$valueTypeName") {
                InMemoryCache<T>()
            }
            @Suppress("UNCHECKED_CAST")
            return cache as Cache<T>
        }

    @OptIn(DelicateCoroutinesApi::class)
    override fun close() {
        context?.unregisterComponentCallbacks(componentCallbacks)

        GlobalScope.launch {
            caches.values.forEach { it.close() }
        }
    }

    override fun onTrimMemory(level: MemoryObserver.Level) {
        caches.forEach { (_, cache) -> cache.onTrimMemory(level) }
    }
}
