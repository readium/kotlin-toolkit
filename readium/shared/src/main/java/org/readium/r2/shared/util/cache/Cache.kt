/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.cache

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.MemoryObserver
import org.readium.r2.shared.util.Try

/**
 * A generic cache for objects of type [V].
 *
 * It implements [MemoryObserver] to flush unused in-memory objects when necessary.
 */
@InternalReadiumApi
public interface Cache<V> : Closeable, MemoryObserver {
    /**
     * Performs an atomic [block] transaction on this cache.
     */
    public suspend fun <T> transaction(block: suspend CacheTransaction<V>.() -> T): T
}

/**
 * An atomic transaction run on a cache for objects of type [V].
 */
@InternalReadiumApi
public interface CacheTransaction<V> {
    /**
     * Gets the current cached value for the given [key].
     */
    public suspend fun get(key: String): V?

    /**
     * Writes the cached [value] for the given [key].
     */
    public suspend fun put(key: String, value: V?)

    /**
     * Clears the cached value for the given [key].
     *
     * @return The cached value if any.
     */
    public suspend fun remove(key: String): V?

    /**
     * Clears all cached values.
     */
    public suspend fun clear()
}

/**
 * Gets the current cached value for the given [key] or creates and caches a new one.
 */
public suspend fun <V> CacheTransaction<V>.getOrPut(key: String, defaultValue: suspend () -> V): V =
    get(key)
        ?: defaultValue().also { put(key, it) }

/**
 * Gets the current cached value for the given [key] or creates and caches a new one.
 */
public suspend fun <V, F> CacheTransaction<V>.getOrTryPut(
    key: String,
    defaultValue: suspend () -> Try<V, F>,
): Try<V, F> =
    get(key)?.let { Try.success(it) }
        ?: defaultValue()
            .onSuccess { put(key, it) }

/**
 * A basic in-memory cache.
 */
@InternalReadiumApi
public class InMemoryCache<V> : Cache<V> {
    private val values = mutableMapOf<String, V>()
    private val mutex = Mutex()

    override suspend fun <T> transaction(block: suspend CacheTransaction<V>.() -> T): T =
        mutex.withLock {
            block(Transaction())
        }

    private inner class Transaction : CacheTransaction<V> {
        override suspend fun get(key: String): V? =
            values[key]

        override suspend fun put(key: String, value: V?) {
            if (value != null) {
                values[key] = value
            } else {
                values.remove(key)
            }
        }

        override suspend fun remove(key: String): V? =
            values.remove(key)

        override suspend fun clear() {
            values.clear()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun close() {
        GlobalScope.launch {
            transaction {
                for ((_, value) in values) {
                    (value as? Closeable)?.close()
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onTrimMemory(level: MemoryObserver.Level) {
        if (level == MemoryObserver.Level.Background) {
            GlobalScope.launch { transaction { clear() } }
        }
    }
}
