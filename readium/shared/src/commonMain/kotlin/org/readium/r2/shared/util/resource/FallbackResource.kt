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
 * Resource that will act as a proxy to a fallback resource if the [originalResource] errors out.
 */
public class FallbackResource(
    private val originalResource: Resource,
    private val fallbackResourceFactory: (ReadError) -> Resource?,
) : Resource {

    override val sourceUrl: AbsoluteUrl? = null

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        withResource { properties() }

    override suspend fun length(): Try<Long, ReadError> =
        withResource { length() }

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> =
        withResource { read(range) }

    override fun close() {
        if (::_resource.isInitialized) {
            _resource.close()
        }
    }

    private lateinit var _resource: Resource

    private suspend fun <T> withResource(action: suspend Resource.() -> Try<T, ReadError>): Try<T, ReadError> {
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
    fallbackResourceFactory: (ReadError) -> Resource?,
): Resource =
    FallbackResource(this, fallbackResourceFactory)

/**
 * Falls back to the given alternative [Resource] when the receiver fails.
 */
public fun Resource.fallback(fallbackResource: Resource): Resource =
    FallbackResource(this) { fallbackResource }
