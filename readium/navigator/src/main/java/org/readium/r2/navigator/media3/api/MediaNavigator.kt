/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Closeable

@ExperimentalReadiumApi
interface MediaNavigator<P : MediaNavigator.Position> : Navigator, Closeable {

    /**
     *  Marker interface for the [position] flow.
     */
    interface Position

    /**
     * State of the player.
     */
    sealed interface State {

        /**
         * The navigator is ready to play.
         */
        interface Ready : State

        /**
         * The end of the media has been reached.
         */
        interface Ended : State

        /**
         * The navigator cannot play because the buffer is starved.
         */
        interface Buffering : State

        /**
         * The navigator cannot play because an error occurred.
         */
        interface Error : State
    }

    /**
     * State of the playback.
     *
     * @param state The current state.
     * @param playWhenReady If the navigator should play as soon as the state is Ready.
     */
    data class Playback(
        val state: State,
        val playWhenReady: Boolean
    )

    /**
     * Indicates the current state of the playback.
     */
    val playback: StateFlow<Playback>

    val position: StateFlow<P>

    /**
     * Resumes the playback at the current location.
     */
    fun play()

    /**
     * Pauses the playback.
     */
    fun pause()

    /**
     * Adapts this navigator to the media3 [Player] interface.
     */
    fun asPlayer(): Player
}
