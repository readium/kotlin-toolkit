/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

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

    return if (bytes != null) {
        MediaType.ofBytes(bytes, mediaTypes = allMediaTypes, fileExtensions = allFileExtensions, sniffers = sniffers)
    } else {
        MediaType.of(mediaTypes = allMediaTypes, fileExtensions = allFileExtensions, sniffers = sniffers)
    }
}
