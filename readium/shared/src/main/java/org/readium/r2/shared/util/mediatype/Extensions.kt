/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import java.io.File
import java.net.HttpURLConnection

@Suppress("UnusedReceiverParameter", "RedundantSuspendModifier", "UNUSED_PARAMETER")
@Deprecated("Use your own solution instead", level = DeprecationLevel.ERROR)
public suspend fun HttpURLConnection.sniffMediaType(
    bytes: (() -> ByteArray)? = null,
    mediaTypes: List<String> = emptyList(),
    fileExtensions: List<String> = emptyList()
): MediaType? = throw NotImplementedError()

@Suppress("UnusedReceiverParameter", "RedundantSuspendModifier", "UNUSED_PARAMETER")
@Deprecated("Use your own solution instead", level = DeprecationLevel.ERROR)
public suspend fun File.mediaType(mediaTypeHint: String? = null): MediaType =
    throw NotImplementedError()
