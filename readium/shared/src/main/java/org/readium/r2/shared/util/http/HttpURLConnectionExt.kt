/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import java.net.HttpURLConnection
import org.readium.r2.shared.extensions.extension
import org.readium.r2.shared.util.mediatype.MediaTypeHints

public operator fun MediaTypeHints.Companion.invoke(
    connection: HttpURLConnection,
    mediaType: String? = null
): MediaTypeHints =
    MediaTypeHints(
        mediaTypes = listOfNotNull(connection.contentType, mediaType),
        fileExtensions = listOfNotNull(
            connection.url.extension
            // TODO: The suggested filename extension, part of the HTTP header `Content-Disposition`.
        )
    )
