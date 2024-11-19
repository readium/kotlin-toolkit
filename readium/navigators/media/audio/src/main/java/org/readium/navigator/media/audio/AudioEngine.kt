/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.audio

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * An audio engine playing a list of items.
 */
@ExperimentalReadiumApi
public interface AudioEngine<S : Configurable.Settings, P : Configurable.Preferences<P>> :
    Configurable<S, P> {

    /**
     * Marker interface for the errors that the [AudioEngine] returns.
     */
    public interface Error : org.readium.r2.shared.util.Error

    /**
     * State of the player.
     */
    public sealed class State {

        /**
         * The player is ready to play.
         */
        public data object Ready : State()

        /**
         * The end of the content has been reached.
         */
        public data object Ended : State()

        /**
         * The engine cannot play because the buffer is starved.
         */
        public data object Buffering : State()

        /**
         * The engine cannot play because an error occurred.
         */
        public data class Failure(val error: Error) : State()
    }

    /**
     * State of the playback.
     *
     * @param state The current state.
     * @param playWhenReady Indicates if the navigator should play as soon as the state is Ready.
     * @param index Index of the reading order item currently being played.
     * @param offset Position of the playback in the current item.
     * @param buffered Position in the current item until which the content is buffered.
     */
    public data class Playback(
        val state: State,
        val playWhenReady: Boolean,
        val index: Int,
        val offset: Duration,
        val buffered: Duration?,
    )

    /**
     * Current state of the playback.
     */
    public val playback: StateFlow<Playback>

    /**
     * Resumes the playback at the current location.
     */
    public fun play()

    /**
     * Pauses the playback.
     */
    public fun pause()

    /**
     * Skips to [offset] in the item at [index].
     */
    public fun skipTo(index: Int, offset: Duration)

    /**
     * Skips [duration] either forward or backward if [duration] is negative.
     */
    public fun skip(duration: Duration)

    /**
     * Skips forward a small increment.
     */
    public fun skipForward()

    /**
     * Skips backward a small increment.
     */
    public fun skipBackward()

    /**
     * Closes the player.
     */
    public fun close()

    /**
     * Adapts this engine to the media3 [Player] interface.
     */
    public fun asPlayer(): Player
}
