/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import java.io.File
import java.net.HttpURLConnection
import org.readium.r2.shared.extensions.extension
import org.readium.r2.shared.resource.DefaultArchiveFactory

/**
 * Resolves the format for this [HttpURLConnection], with optional extra file extension and media type
 * hints.
 */
@Deprecated(
    "Use the MediaTypeRetriever extension instead.",
    replaceWith = ReplaceWith(
        "mediaTypeRetriever.retrieve(connection, bytes, mediaTypes, fileExtensions)",
        "org.readium.r2.shared.util.http.retrieve"
    ),
    level = DeprecationLevel.ERROR
)
suspend fun HttpURLConnection.sniffMediaType(
    bytes: (() -> ByteArray)? = null,
    mediaTypes: List<String> = emptyList(),
    fileExtensions: List<String> = emptyList(),
    sniffers: List<Sniffer> = MediaType.sniffers
): MediaType? {
    val allMediaTypes = mediaTypes.toMutableList()
    val allFileExtensions = fileExtensions.toMutableList()

    // The value of the `Content-Type` HTTP header.
    contentType?.let {
        allMediaTypes.add(0, it)
    }

    // The URL file extension.
    url.extension?.let {
        allFileExtensions.add(0, it)
    }

    // TODO: The suggested filename extension, part of the HTTP header `Content-Disposition`.

    val mediaTypeRetriever = MediaTypeRetriever(sniffers = sniffers)

    return if (bytes != null) {
        mediaTypeRetriever.doRetrieve(
            {
                BytesSnifferContextFactory(DefaultArchiveFactory())
                    .createContext(bytes.invoke(), mediaTypes = allMediaTypes, fileExtensions = allFileExtensions)
            },
            mediaTypes = allMediaTypes,
            fileExtensions = allFileExtensions
        )
    } else {
        mediaTypeRetriever.retrieve(mediaTypes = allMediaTypes, fileExtensions = allFileExtensions)
    }
}

/**
* Sniffs the media type of the file.
*
* If unknown, fallback on `MediaType.BINARY`.
*/
@Deprecated(
    "Use MediaTypeRetriever explicitly.",
    replaceWith = ReplaceWith("mediaTypeRetriever.retrieve(mediaType = mediaTypeHint)"),
    level = DeprecationLevel.ERROR
)
suspend fun File.mediaType(mediaTypeHint: String? = null): MediaType =
    MediaTypeRetriever().retrieve(this, mediaType = mediaTypeHint) ?: MediaType.BINARY
