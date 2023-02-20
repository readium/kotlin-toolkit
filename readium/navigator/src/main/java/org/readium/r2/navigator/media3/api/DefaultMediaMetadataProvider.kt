/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import org.readium.r2.shared.publication.Publication

/**
 * Builds a [MediaMetadataFactory] which will use the given title, author and cover,
 * and fall back on what is in the publication.
 */
class DefaultMediaMetadataProvider(
    private val title: String? = null,
    private val author: String? = null,
    private val cover: ByteArray? = null
) : MediaMetadataProvider {

    override fun createMetadataFactory(publication: Publication): MediaMetadataFactory {
        return DefaultMediaMetadataFactory(publication, title, author, cover)
    }
}
