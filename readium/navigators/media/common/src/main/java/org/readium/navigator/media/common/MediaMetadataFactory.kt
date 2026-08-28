/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.common

import androidx.media3.common.MediaMetadata

/**
 * Factory for the [MediaMetadata] associated with the publication and its resources.
 *
 * The metadata are used for example in the media-style Android notification.
 */
public interface MediaMetadataFactory {

    /**
     * Creates the [MediaMetadata] for the whole publication.
     */
    public suspend fun publicationMetadata(): MediaMetadata

    /**
     * Creates the [MediaMetadata] for the reading order resource at the given [index].
     */
    public suspend fun resourceMetadata(index: Int): MediaMetadata
}
