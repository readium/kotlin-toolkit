/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError

/**
 * Protects the access to a wrapped resource with a mutex to make it thread-safe.
 */
public class SynchronizedResource(
    private val resource: Resource,
) : Resource {

    // This doesn't use `Resource by resource` to avoid forgetting the synchronization for a future API.

    private val mutex = Mutex()

    override val sourceUrl: AbsoluteUrl? get() = resource.sourceUrl

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        mutex.withLock { resource.properties() }

    override suspend fun length(): Try<Long, ReadError> =
        mutex.withLock { resource.length() }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> =
        mutex.withLock { resource.read(range) }

    override fun close() {
        resource.close()
    }

    override fun toString(): String =
        "${javaClass.simpleName}($resource)"
}

/**
 * Wraps this resource in a [SynchronizedResource] to protect the access from multiple threads.
 */
public fun Resource.synchronized(): SynchronizedResource =
    SynchronizedResource(this)
