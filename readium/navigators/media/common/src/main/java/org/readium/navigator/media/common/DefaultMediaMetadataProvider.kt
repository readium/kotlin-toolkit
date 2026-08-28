/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.common

import android.net.Uri
import org.readium.r2.shared.publication.Publication

/**
 * Builds a [MediaMetadataFactory] which will use the given title, author and cover,
 * and fall back on what is in the publication.
 */
public class DefaultMediaMetadataProvider(
    private val title: String? = null,
    private val author: String? = null,
    private val cover: Uri? = null,
) : MediaMetadataProvider {

    override fun createMetadataFactory(publication: Publication): MediaMetadataFactory {
        return DefaultMediaMetadataFactory(publication, title, author, cover)
    }
}
