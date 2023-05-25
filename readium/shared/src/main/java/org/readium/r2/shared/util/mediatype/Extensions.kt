/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import com.github.kittinunf.fuel.core.Response
import java.io.File
import java.net.HttpURLConnection
import org.readium.r2.shared.extensions.extension

/**
 * Resolves the format for this [HttpURLConnection], with optional extra file extension and media type
 * hints.
 */
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

    val mediaTypeRetriever = MediaTypeRetrieverInternal(sniffers)

    return if (bytes != null) {
        mediaTypeRetriever.of(bytes, mediaTypes = allMediaTypes, fileExtensions = allFileExtensions)
    } else {
        mediaTypeRetriever.of(mediaTypes = allMediaTypes, fileExtensions = allFileExtensions)
    }
}

/**
 * Resolves the format for this [Response], with optional extra file extension and media type
 * hints.
 */
suspend fun Response.sniffMediaType(
    mediaTypes: List<String> = emptyList(),
    fileExtensions: List<String> = emptyList(),
    sniffers: List<Sniffer> = MediaType.sniffers
): MediaType? {
    val allMediaTypes = mediaTypes.toMutableList()
    val allFileExtensions = fileExtensions.toMutableList()

    // The value of the `Content-Type` HTTP header.
    allMediaTypes.addAll(0, headers["Content-Type"])

    // The URL file extension.
    url.extension?.let {
        allFileExtensions.add(0, it)
    }

    val mediaTypeRetriever = MediaTypeRetrieverInternal(sniffers)
    val bytes: () -> ByteArray = { data }

    // TODO: The suggested filename extension, part of the HTTP header `Content-Disposition`.

    return mediaTypeRetriever.of(bytes, mediaTypes = allMediaTypes, fileExtensions = allFileExtensions)
}


/**
* Sniffs the media type of the file.
*
* If unknown, fallback on `MediaType.BINARY`.
*/
suspend fun File.mediaType(mediaTypeHint: String? = null): MediaType =
    MediaTypeRetriever().ofFile(this, mediaType = mediaTypeHint) ?: MediaType.BINARY
