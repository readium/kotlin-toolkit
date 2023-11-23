/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.tryRecover

internal class BlobResourceAdapter(
    private val blob: Blob,
    properties: Resource.Properties,
    private val mediaTypeRetriever: MediaTypeRetriever
) : Resource, Blob by blob {

    private val properties: Resource.Properties =
        properties.copy { mediaType = mediaTypeRetriever.retrieve(MediaTypeHints(properties)) }

    override suspend fun mediaType(): Try<MediaType, ReadError> =
        mediaTypeRetriever.retrieve(
            hints = MediaTypeHints(properties),
            blob = blob
        ).tryRecover { error ->
            when (error) {
                is MediaTypeSnifferError.Read ->
                    Try.failure(error.cause)
                MediaTypeSnifferError.NotRecognized ->
                    Try.success(MediaType.BINARY)
            }
        }

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        Try.success(
            Resource.Properties(properties)
        )
}

internal class BlobContainerAdapter(
    private val container: Container<Blob>,
    private val properties: Map<Url, Resource.Properties>,
    private val mediaTypeRetriever: MediaTypeRetriever
) : Container<Resource> {
    override val entries: Set<Url> =
        container.entries

    override fun get(url: Url): Resource? {
        val blob = container[url] ?: return null

        val resourceProperties = properties[url] ?: Resource.Properties()

        return BlobResourceAdapter(blob, resourceProperties, mediaTypeRetriever)
    }

    override suspend fun close() {
        container.close()
    }
}
