/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.FileBlob
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.resource.BlobResourceAdapter
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType

public class FileResourceFactory(
    private val mediaTypeRetriever: MediaTypeRetriever = MediaTypeRetriever()
) : ResourceFactory {

    override suspend fun create(
        url: AbsoluteUrl,
        mediaType: MediaType?
    ): Try<Resource, ResourceFactory.Error> {
        val file = url.toFile()
            ?: return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))

        val blob = FileBlob(file)

        val properties =
            Resource.Properties(
                Resource.Properties.Builder()
                    .also {
                        it.filename = url.filename
                        it.mediaType = mediaType
                    }
            )

        val resource =
            BlobResourceAdapter(
                blob,
                properties,
                mediaTypeRetriever
            )

        return Try.success(resource)
    }
}
