package org.readium.r2.navigator.media2

import androidx.media2.common.MediaMetadata
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
sealed class MediaNavigatorPlayback {

    object Finished: MediaNavigatorPlayback()

    object Error: MediaNavigatorPlayback()

    data class Playing(
        val paused: Boolean,
        val currentItem: MediaMetadata,
        val currentPosition: Duration,
        val bufferedPosition: Duration
    ) : MediaNavigatorPlayback()
}
