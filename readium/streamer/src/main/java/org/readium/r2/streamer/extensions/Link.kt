/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.extensions

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.util.mediatype.MediaType

internal suspend fun Container.Entry.toLink(mediaType: MediaType? = null): Link =
    Link(
        href = url,
        mediaType = mediaType ?: mediaType().getOrNull()
    )
