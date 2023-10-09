/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Protects the access to a wrapped resource with a mutex to make it thread-safe.
 */
public class SynchronizedResource(
    private val resource: Resource
) : Resource {

    // This doesn't use `Resource by resource` to avoid forgetting the synchronization for a future API.

    private val mutex = Mutex()

    override val source: AbsoluteUrl? get() = resource.source

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        mutex.withLock { resource.properties() }

    override suspend fun mediaType(): ResourceTry<MediaType> =
        mutex.withLock { resource.mediaType() }

    override suspend fun length(): ResourceTry<Long> =
        mutex.withLock { resource.length() }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        mutex.withLock { resource.read(range) }

    override suspend fun close() {
        mutex.withLock { resource.close() }
    }

    override fun toString(): String =
        "${javaClass.simpleName}($resource)"
}

/**
 * Wraps this resource in a [SynchronizedResource] to protect the access from multiple threads.
 */
public fun Resource.synchronized(): SynchronizedResource =
    SynchronizedResource(this)
