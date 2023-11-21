/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import android.content.ContentResolver
import android.provider.MediaStore
import org.readium.r2.shared.extensions.queryProjection
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ContentBlob
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.resource.BlobResourceAdapter
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType
import org.readium.r2.shared.util.toUri

/**
 * Creates [ContentBlob]s.
 */
public class ContentResourceFactory(
    private val contentResolver: ContentResolver,
    private val mediaTypeRetriever: MediaTypeRetriever
) : ResourceFactory {

    override suspend fun create(
        url: AbsoluteUrl,
        mediaType: MediaType?
    ): Try<Resource, ResourceFactory.Error> {
        if (!url.isContent) {
            return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))
        }

        val blob = ContentBlob(url.toUri(), contentResolver)

        val filename =
            contentResolver.queryProjection(url.uri, MediaStore.MediaColumns.DISPLAY_NAME)

        val properties =
            Resource.Properties(
                Resource.Properties.Builder()
                    .also {
                        it.filename = filename
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
