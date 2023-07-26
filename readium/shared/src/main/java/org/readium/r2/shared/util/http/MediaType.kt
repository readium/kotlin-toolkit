/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import java.net.HttpURLConnection
import org.readium.r2.shared.extensions.extension
import org.readium.r2.shared.resource.DefaultArchiveFactory
import org.readium.r2.shared.util.mediatype.BytesSnifferContextFactory
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * Resolves the format for this [HttpURLConnection], with optional extra file extension and media type
 * hints.
 */
public suspend fun MediaTypeRetriever.retrieve(
    connection: HttpURLConnection,
    bytes: (() -> ByteArray)?,
    mediaTypes: List<String>,
    fileExtensions: List<String>,
): MediaType? {
    val allMediaTypes = mediaTypes.toMutableList()
    val allFileExtensions = fileExtensions.toMutableList()

    // The value of the `Content-Type` HTTP header.
    connection.contentType?.let {
        allMediaTypes.add(0, it)
    }

    // The URL file extension.
    connection.url.extension?.let {
        allFileExtensions.add(0, it)
    }

    // TODO: The suggested filename extension, part of the HTTP header `Content-Disposition`.

    return if (bytes != null) {
        doRetrieve(
            {
                BytesSnifferContextFactory(DefaultArchiveFactory())
                    .createContext(bytes.invoke(), mediaTypes = allMediaTypes, fileExtensions = allFileExtensions)
            },
            mediaTypes = allMediaTypes,
            fileExtensions = allFileExtensions
        )
    } else {
        retrieve(mediaTypes = allMediaTypes, fileExtensions = allFileExtensions)
    }
}

/**
 * Resolves the format for this [HttpURLConnection], with optional extra file extension and media type
 * hints.
 */
public suspend fun MediaTypeRetriever.retrieve(
    connection: HttpURLConnection,
    bytes: (() -> ByteArray)? = null,
    mediaType: String? = null,
    fileExtension: String? = null
): MediaType? = retrieve(connection, bytes, listOfNotNull(mediaType), listOfNotNull(fileExtension))
