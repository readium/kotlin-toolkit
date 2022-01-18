package org.readium.navigator.media2

import androidx.media2.common.MediaMetadata

interface MediaMetadataFactory {

    val fillPublicationMetadata: suspend (MediaMetadata.Builder).() -> Unit

    val fillResourceMetadata: suspend (MediaMetadata.Builder).(Int) -> Unit
}