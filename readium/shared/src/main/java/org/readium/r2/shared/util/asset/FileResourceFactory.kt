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
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.resource.GuessMediaTypeResourceAdapter
import org.readium.r2.shared.util.resource.KnownMediaTypeResourceAdapter
import org.readium.r2.shared.util.resource.Resource

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

        val resource = mediaType
            ?.let { KnownMediaTypeResourceAdapter(blob, it) }
            ?: GuessMediaTypeResourceAdapter(
                blob,
                mediaTypeRetriever,
                MediaTypeHints(fileExtension = file.extension)
            )

        return Try.success(resource)
    }
}
