package org.readium.r2.navigator.media2

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Try
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
    val playback: StateFlow<Playback>

    /**
     * Resumes or start the playback at the current location.
     */
    suspend fun play(): Try<Unit, Exception>

    /**
     * Pauses the playback.
     */
    suspend fun pause(): Try<Unit, Exception>

    /**
     * Seeks to the given time at the given resource.
     */

    suspend fun seek(index: Int, position: Duration): Try<Unit, Exception>

    /**
     * Skips to a little amount of time later.
     */

    suspend fun goForward(): Try<Unit, Exception>

    /**
     * Skips to a little amount of time before.
     */

    suspend fun goBackward(): Try<Unit, Exception>

    /**
     * Seeks to the beginning of the given link.
     */

    suspend fun go(link: Link): Try<Unit, Exception>

    /**
     * Seeks to the given locator.
     */

    suspend fun go(locator: Locator): Try<Unit, Exception>

    /**
     * Sets the speed of the media playback.
     *
     * Normal speed is 1.0 and 0.0 is incorrect.
     */
    suspend fun setPlaybackRate(rate: Double): Try<Unit, Exception>

    /**
     * Stops the playback.
     *
     * Compared to [pause], the navigator may clear its state in whatever way is appropriate. For
     * example, recovering a player's resources.
     */
    fun close()

    @ExperimentalTime
    data class Playback(
        val state: State,
        val rate: Double,
        val currentIndex: Int,
        val currentLink: Link,
        val currentPosition: Duration,
        val bufferedPosition: Duration,
    ) {

        enum class State {
            Playing,
            Paused,
            Finished,
            Error
        }
    }

    sealed class Exception
        : java.lang.Exception()
}
