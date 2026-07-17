/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import android.content.ContentResolver
import java.io.File
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.use

/**
 * Creates an [AssetRetriever] with the default Android resource factory, archive opener and
 * format sniffer.
 *
 * @param contentResolver content resolver to use to support the content scheme.
 * @param httpClient HTTP client to use to support the http scheme.
 */
public fun AssetRetriever(
    contentResolver: ContentResolver,
    httpClient: HttpClient,
): AssetRetriever =
    AssetRetriever(
        DefaultResourceFactory(contentResolver, httpClient),
        DefaultArchiveOpener(),
        DefaultFormatSniffer()
    )

/**
 * Retrieves an asset from a local file.
 */
public suspend fun AssetRetriever.retrieve(
    file: File,
    formatHints: FormatHints = FormatHints(),
): Try<Asset, AssetRetriever.RetrieveError> =
    retrieve(FileResource(file), formatHints)

/**
 * Retrieves an asset from a local file.
 */
public suspend fun AssetRetriever.retrieve(
    file: File,
    mediaType: MediaType,
): Try<Asset, AssetRetriever.RetrieveError> =
    retrieve(file, FormatHints(mediaType = mediaType))

/**
 * Sniffs the format of a file content.
 */
public suspend fun AssetRetriever.sniffFormat(
    file: File,
    hints: FormatHints = FormatHints(),
): Try<Format, AssetRetriever.RetrieveError> =
    FileResource(file).use { sniffFormat(it, hints) }
