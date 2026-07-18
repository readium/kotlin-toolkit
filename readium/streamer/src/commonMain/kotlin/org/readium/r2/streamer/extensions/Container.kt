/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.appendToFilename
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.shared.util.use

internal fun ResourceAsset.toContainer(): Container<Resource> {
    // Historically, the reading order of a standalone file contained a single link with the
    // HREF "/$assetName". This was fragile if the asset named changed, or was different on
    // other devices. To avoid this, we now use a single link with the HREF
    // "publication.extension".
    val extension = format
        .fileExtension

    return SingleResourceContainer(
        Url(extension.appendToFilename("publication"))!!,
        resource
    )
}

internal suspend fun AssetRetriever.sniffContainerEntries(
    container: Container<Resource>,
    filter: (Url) -> Boolean,
): Try<Map<Url, Format>, ReadError> =
    container
        .filter(filter)
        .fold(Try.success(emptyMap())) { acc: Try<Map<Url, Format>, ReadError>, url ->
            when (acc) {
                is Try.Failure ->
                    acc

                is Try.Success ->
                    container[url]!!.use { resource ->
                        sniffFormat(resource).fold(
                            onSuccess = {
                                Try.success(acc.value + (url to it))
                            },
                            onFailure = {
                                when (it) {
                                    is AssetRetriever.RetrieveError.FormatNotSupported -> acc
                                    is AssetRetriever.RetrieveError.Reading -> Try.failure(it.cause)
                                }
                            }
                        )
                    }
            }
        }
