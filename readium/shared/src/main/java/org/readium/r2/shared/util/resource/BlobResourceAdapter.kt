/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
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
