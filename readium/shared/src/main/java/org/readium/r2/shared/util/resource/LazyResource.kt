/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError

/**
 * Wraps a [Resource] which will be created only when first accessing one of its members.
 */
public open class LazyResource(
    override val sourceUrl: AbsoluteUrl? = null,
    private val factory: suspend () -> Resource,
) : Resource {

    private lateinit var _resource: Resource

    protected suspend fun resource(): Resource {
        if (!::_resource.isInitialized) {
            _resource = factory()
        }

        return _resource
    }

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        resource().properties()

    override suspend fun length(): Try<Long, ReadError> =
        resource().length()

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> =
        resource().read(range)

    override fun close() {
        if (::_resource.isInitialized) {
            _resource.close()
        }
    }

    override fun toString(): String =
        if (::_resource.isInitialized) {
            "${javaClass.simpleName}($_resource)"
        } else {
            "${javaClass.simpleName}(...)"
        }
}

public fun <R : Resource> Resource.flatMap(transform: suspend (Resource) -> R): LazyResource =
    LazyResource { transform(this) }
