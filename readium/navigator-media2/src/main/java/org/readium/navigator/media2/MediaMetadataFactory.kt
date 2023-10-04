package org.readium.navigator.media2

import androidx.media2.common.MediaMetadata

/**
 * Factory for the [MediaMetadata] associated with the publication and its resources.
 *
 * The metadata are used for example in the media-style Android notification.
 */
@Deprecated("Use the new MediaMetadataFactory from the readium-navigator-media-audio module.")
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
