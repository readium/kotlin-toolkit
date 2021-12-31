package org.readium.r2.navigator.media2

import org.readium.r2.shared.publication.Link
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
sealed class MediaNavigatorPlayback {

    object Finished: MediaNavigatorPlayback()

    object Error: MediaNavigatorPlayback()

    data class Playing(
        val paused: Boolean,
        val currentIndex: Int,
        val currentLink: Link,
        val currentPosition: Duration,
        val bufferedPosition: Duration
    ) : MediaNavigatorPlayback()
}
