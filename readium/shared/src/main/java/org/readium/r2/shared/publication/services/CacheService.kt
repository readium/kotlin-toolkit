/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.MemoryObserver
import kotlin.reflect.KClass

/**
 * Provides publication-related in-memory caches.
 */
@InternalReadiumApi
interface CacheService : Publication.Service {
    /**
     * Gets or creates the cache for objects of given type.
     */
    fun <T : Any> cacheOf(valueType: KClass<T>): Cache<T>
}

@InternalReadiumApi
val PublicationServicesHolder.cacheService: CacheService?
    get() = findService(CacheService::class)

/** Factory to build a [CacheService]. */
@InternalReadiumApi
var Publication.ServicesBuilder.cacheServiceFactory: ServiceFactory?
    get() = get(CacheService::class)
    set(value) = set(CacheService::class, value)

@InternalReadiumApi
class DefaultCacheService : CacheService {
    private val caches = mutableMapOf<String, Cache<*>>()

    override fun <T : Any> cacheOf(valueType: KClass<T>): Cache<T> = synchronized(this) {
        val valueTypeName = requireNotNull(valueType.qualifiedName)
        val cache = caches.getOrPut(valueTypeName) {
            InMemoryCache<T>()
        }
        @Suppress("UNCHECKED_CAST")
        cache as Cache<T>
    }

    override fun close() {
        caches.values.forEach { it.close() }
    }

    override fun onTrimMemory(level: MemoryObserver.Level) {
        caches.forEach { (_, cache) -> cache.onTrimMemory(level) }
    }
}

@InternalReadiumApi
interface Cache<T>: Closeable, MemoryObserver {
    suspend fun get(key: String): T?
    suspend fun put(key: String, value: T?)
    suspend fun remove(key: String): T?
    suspend fun clear()
    override fun close() {}
    override fun onTrimMemory(level: MemoryObserver.Level) {}
}

@InternalReadiumApi
suspend inline fun <T> Cache<T>.getOrPut(key: String, defaultValue: () -> T): T =
    get(key)
        ?: defaultValue().also { put(key, it) }

/**
 * A basic in-memory cache
 */
@InternalReadiumApi
class InMemoryCache<T> : Cache<T> {
    private val values = mutableMapOf<String, T>()
    private val mutex = Mutex()

    override suspend fun get(key: String): T? = mutex.withLock {
        values[key]
    }

    override suspend fun put(key: String, value: T?): Unit = mutex.withLock {
        if (value != null) {
            values[key] = value
        } else {
            values.remove(key)
        }
    }

    override suspend fun remove(key: String): T? = mutex.withLock {
        values.remove(key)
    }

    override suspend fun clear() = mutex.withLock {
        values.clear()
    }

    override fun close() {
        for (value in values) {
            (value as? Closeable)?.close()
        }
    }

    override fun onTrimMemory(level: MemoryObserver.Level) {
        if (level == MemoryObserver.Level.Critical) {
            runBlocking { clear() }
        }
    }
}