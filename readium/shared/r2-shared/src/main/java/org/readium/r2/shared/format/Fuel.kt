/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import com.github.kittinunf.fuel.core.Response
import org.readium.r2.shared.extensions.extension

/** Fuel Format extensions */

/**
 * Resolves the format for this [Response], with optional extra file extension and media type
 * hints.
 */
suspend fun Response.sniffFormat(mediaTypes: List<String> = emptyList(), fileExtensions: List<String> = emptyList(), sniffers: List<FormatSniffer> = Format.sniffers): Format? {
    val allMediaTypes = mediaTypes.toMutableList()
    val allFileExtensions = fileExtensions.toMutableList()

    // The value of the `Content-Type` HTTP header.
    (headers["Content-Type"] ?: headers["content-type"])?.let {
        allMediaTypes.addAll(0, it)
    }

    // The URL file extension.
    url.extension?.let {
        allFileExtensions.add(0, it)
    }

    // TODO: The suggested filename extension, part of the HTTP header `Content-Disposition`.

    return Format.ofBytes({ data }, mediaTypes = allMediaTypes, fileExtensions = allFileExtensions, sniffers = sniffers)
}
