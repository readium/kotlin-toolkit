/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.MemoryObserver

/**
 * A generic cache for objects of type [V].
 *
 * It implements [MemoryObserver] to flush unused in-memory objects when necessary.
 */
@InternalReadiumApi
interface Cache<V>: Closeable, MemoryObserver {
    /**
     * Gets the current cached value for the given [key].
     */
    suspend fun get(key: String): V?

    /**
     * Writes the cached [value] for the given [key].
     */
    suspend fun put(key: String, value: V?)

    /**
     * Clears the cached value for the given [key].
     *
     * @return The cached value if any.
     */
    suspend fun remove(key: String): V?

    /**
     * Clears all cached values.
     */
    suspend fun clear()

    /**
     * Performs an atomic [action] on this cache.
     */
    suspend fun <T> synchronized(action: suspend () -> T): T
}

/**
 * Gets the current cached value for the given [key] or creates and caches a new one.
 */
@InternalReadiumApi
suspend inline fun <V> Cache<V>.getOrPut(key: String, crossinline defaultValue: suspend () -> V): V = synchronized {
    get(key)
        ?: defaultValue().also { put(key, it) }
}

/**
 * A basic in-memory cache.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@InternalReadiumApi
class InMemoryCache<V> : Cache<V> {
    private val values = mutableMapOf<String, V>()

    override suspend fun get(key: String): V? = synchronized {
        values[key]
    }

    override suspend fun put(key: String, value: V?) {
        synchronized {
            if (value != null) {
                values[key] = value
            } else {
                values.remove(key)
            }
        }
    }

    override suspend fun remove(key: String): V? = synchronized {
        values.remove(key)
    }

    override suspend fun clear() {
        synchronized {
            values.clear()
        }
    }

    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    override suspend fun <T> synchronized(action: suspend () -> T): T = withContext(dispatcher) {
        action()
    }

    override fun close() {
        for ((_, value) in values) {
            (value as? Closeable)?.close()
        }
    }

    override fun onTrimMemory(level: MemoryObserver.Level) {
        if (level == MemoryObserver.Level.Critical) {
            runBlocking { clear() }
        }
    }
}
