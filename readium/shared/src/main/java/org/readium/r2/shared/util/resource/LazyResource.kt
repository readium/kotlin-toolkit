/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Wraps a [Resource] which will be created only when first accessing one of its members.
 */
public open class LazyResource<R : Resource>(
    override val source: AbsoluteUrl? = null,
    private val factory: suspend () -> R
) : Resource {

    private lateinit var _resource: R

    protected suspend fun resource(): R {
        if (!::_resource.isInitialized) {
            _resource = factory()
        }

        return _resource
    }

    override suspend fun mediaType(): ResourceTry<MediaType> =
        resource().mediaType()

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        resource().properties()

    override suspend fun length(): ResourceTry<Long> =
        resource().length()

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        resource().read(range)

    override suspend fun close() {
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

public fun <R : Resource> Resource.flatMap(transform: suspend (Resource) -> R): LazyResource<R> =
    LazyResource { transform(this) }
