/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import org.readium.r2.shared.extensions.extension
import java.net.HttpURLConnection

/** HttpURLConnection Format extensions */

/** Sniffs the format for this [HttpURLConnection], using the default format sniffers. */
val HttpURLConnection.format: Format? get() = sniffFormat()

/**
 * Resolves the format for this [HttpURLConnection], with optional extra file extension and media type
 * hints.
 */
fun HttpURLConnection.sniffFormat(bytes: (() -> ByteArray)? = null, mediaTypes: List<String> = emptyList(), fileExtensions: List<String> = emptyList(), sniffers: List<FormatSniffer> = Format.sniffers): Format? {
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
        Format.of(bytes = bytes, mediaTypes = allMediaTypes, fileExtensions = allFileExtensions, sniffers = sniffers)
    } else {
        Format.of(mediaTypes = allMediaTypes, fileExtensions = allFileExtensions, sniffers = sniffers)
    }
}
