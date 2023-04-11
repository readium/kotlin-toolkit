/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.audio

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * An audio engine playing a list of items.
 */
@ExperimentalReadiumApi
interface AudioEngine<S : Configurable.Settings, P : Configurable.Preferences<P>> :
    Configurable<S, P> {

    /**
     * Marker interface for the errors that the [AudioEngine] returns.
     */
    interface Error

    /**
     * State of the player.
     */
    sealed class State {

        /**
         * The player is ready to play.
         */
        object Ready : State()

        /**
         * The end of the content has been reached.
         */
        object Ended : State()

        /**
         * The engine cannot play because the buffer is starved.
         */
        object Buffering : State()

        /**
         * The engine cannot play because an error occurred.
         */
        data class Error(val error: AudioEngine.Error) : State()
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
    data class Playback(
        val state: State,
        val playWhenReady: Boolean,
        val index: Int,
        val offset: Duration,
        val buffered: Duration?
    )

    /**
     * Current state of the playback.
     */
    val playback: StateFlow<Playback>

    /**
     * Resumes the playback at the current location.
     */
    fun play()

    /**
     * Pauses the playback.
     */
    fun pause()

    /**
     * Seeks to [position] in the item at [index].
     */
    fun seek(index: Int, position: Duration)

    /**
     * Seeks by [offset] either forward or backward if [offset] is negative.
     */
    fun seekBy(offset: Duration)

    /**
     * Seeks by a small increment forward.
     */
    fun seekForward()

    /**
     * Seeks by a small increment backward.
     */
    fun seekBackward()

    /**
     * Closes the player.
     */
    fun close()

    /**
     * Adapts this engine to the media3 [Player] interface.
     */
    fun asPlayer(): Player
}
