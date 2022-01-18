package org.readium.navigator.media2

import androidx.media2.common.MediaMetadata

@ExperimentalMedia2
interface MediaMetadataFactory {

    suspend fun publicationMetadata(): MediaMetadata

    suspend fun resourceMetadata(index: Int): MediaMetadata
}