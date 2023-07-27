/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Wraps a [Resource] which will be created only when first accessing one of its members.
 */
public class LazyResource(
    override val source: Url? = null,
    private val factory: suspend () -> Resource
) : Resource {

    private lateinit var _resource: Resource

    private suspend fun resource(): Resource {
        if (!::_resource.isInitialized)
            _resource = factory()

        return _resource
    }

    override suspend fun mediaType(): ResourceTry<MediaType?> =
        resource().mediaType()

    override suspend fun name(): ResourceTry<String?> =
        resource().name()

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        resource().properties()

    override suspend fun length(): ResourceTry<Long> =
        resource().length()

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        resource().read(range)

    override suspend fun close() {
        if (::_resource.isInitialized)
            _resource.close()
    }

    override fun toString(): String =
        if (::_resource.isInitialized) {
            "${javaClass.simpleName}($_resource)"
        } else {
            "${javaClass.simpleName}(...)"
        }
}

public fun Resource.flatMap(transform: suspend (Resource) -> Resource): Resource =
    LazyResource { transform(this) }