package org.readium.r2.navigator.media2

import androidx.media2.common.MediaMetadata
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * A navigator rendering an audio or video publication.
 */
@ExperimentalAudiobook
@OptIn(ExperimentalTime::class)
interface MediaNavigator : Navigator {

    /**
     * Indicates the navigator current state.
     */
    val playback: StateFlow<MediaNavigatorPlayback?>

    val playlist: List<MediaMetadata>?

    /**
     * Resumes or start the playback at the current location.
     */
    suspend fun play(): MediaNavigatorResult

    /**
     * Pauses the playback.
     */
    suspend fun pause(): MediaNavigatorResult

    suspend fun seek(itemIndex: Int, position: Duration): MediaNavigatorResult

    suspend fun goForward(): MediaNavigatorResult

    suspend fun goBackward(): MediaNavigatorResult

    suspend fun go(link: Link): MediaNavigatorResult

    suspend fun go(locator: Locator): MediaNavigatorResult

    /**
     * Sets the speed of the media playback.
     *
     * Normal speed is 1.0 and 0.0 is incorrect.
     */
    suspend fun setPlaybackRate(rate: Double): MediaNavigatorResult

    /**
     * Stops the playback.
     *
     * Compared to [pause], the navigator may clear its state in whatever way is appropriate. For
     * example, recovering a player's resources.
     */
    fun close()
}
