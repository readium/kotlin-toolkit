/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.extensions

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ClosedContainer
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use

internal suspend fun ClosedContainer<Resource>.linkForUrl(
    url: Url,
    mediaType: MediaType? = null
): Link =
    Link(
        href = url,
        mediaType = mediaType ?: get(url)?.use { it.mediaType().getOrNull() }
    )

internal suspend fun Resource.toLink(url: Url, mediaType: MediaType? = null): Link =
    Link(
        href = url,
        mediaType = mediaType ?: this.mediaType().getOrNull()
    )
