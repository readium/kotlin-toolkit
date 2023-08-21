/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Resource that will act as a proxy to a fallback resource if the [originalResource] errors out.
 */
public class FallbackResource(
    private val originalResource: Resource,
    private val fallbackResourceFactory: (Resource.Exception) -> Resource?
) : Resource {

    override val source: Url? = null

    override suspend fun mediaType(): ResourceTry<MediaType> =
        withResource { mediaType() }

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        withResource { properties() }

    override suspend fun length(): ResourceTry<Long> =
        withResource { length() }

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        withResource { read(range) }

    override suspend fun close() {
        if (::_resource.isInitialized) {
            _resource.close()
        }
    }

    private lateinit var _resource: Resource

    private suspend fun <T> withResource(action: suspend Resource.() -> ResourceTry<T>): ResourceTry<T> {
        if (::_resource.isInitialized) {
            return _resource.action()
        }

        var resource = originalResource

        var result = resource.action()
        result.onFailure { error ->
            fallbackResourceFactory(error)?.let { fallbackResource ->
                resource = fallbackResource
                result = resource.action()
            }
        }

        _resource = resource
        return result
    }
}

/**
 * Falls back to alternative resources when the receiver fails.
 */
public fun Resource.fallback(
    fallbackResourceFactory: (Resource.Exception) -> Resource?
): Resource =
    FallbackResource(this, fallbackResourceFactory)

/**
 * Falls back to the given alternative [Resource] when the receiver fails.
 */
public fun Resource.fallback(fallbackResource: Resource): Resource =
    FallbackResource(this) { fallbackResource }
