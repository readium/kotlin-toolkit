/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Resource that will act as a proxy to a fallback resource if the [originalResource] errors out.
 */
public class FallbackResource(
    private val originalResource: Resource,
    private val fallbackResourceFactory: (Resource.Exception) -> Resource
) : Resource {

    private val coroutineScope =
        CoroutineScope(Dispatchers.Default)

    private val resource: Deferred<Resource> =
        coroutineScope.async {
            when (val result = originalResource.length()) {
                is Try.Success -> originalResource
                is Try.Failure -> fallbackResourceFactory(result.value)
            }
        }

    override val url: Url? get() = originalResource.url

    override suspend fun name(): ResourceTry<String?> =
        resource.await().name()

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        resource.await().properties()

    override suspend fun mediaType(): ResourceTry<MediaType?> =
        resource.await().mediaType()

    override suspend fun length(): ResourceTry<Long> =
        resource.await().length()

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        resource.await().read(range)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun close() {
        coroutineScope.cancel()
        if (resource.isCompleted) {
            resource.getCompleted().close()
        }
    }
}

/**
 * Falls back to alternative resources when the receiver fails.
 */
public fun Resource.fallback(
    fallbackResourceFactory: (Resource.Exception) -> Resource
): Resource =
    FallbackResource(this, fallbackResourceFactory)

/**
 * Falls back to the given alternative [Resource] when the receiver fails.
 */
public fun Resource.fallback(fallbackResource: Resource): Resource =
    FallbackResource(this) { fallbackResource }
