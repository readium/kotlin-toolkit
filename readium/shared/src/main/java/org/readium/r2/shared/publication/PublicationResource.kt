/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

internal class PublicationResource(
    private val container: Container,
    private val link: Link
) : Resource {

    override val source: Url? = null

    override suspend fun mediaType(): ResourceTry<MediaType?> =
        link.type
            ?.let { MediaType.parse(it) }
            ?.let { ResourceTry.success(it) }
            ?: withResource { mediaType() }

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

        var resource = container.get(link.href)
        var result = resource.action()
        if (result.failureOrNull() is Resource.Exception.NotFound) {
            // Try again after removing query and fragment.
            resource = link.href
                .takeWhile { it !in "#?" }
                .let { container.get(it) }
            result = resource.action()
        }
        _resource = resource
        return result
    }
}
