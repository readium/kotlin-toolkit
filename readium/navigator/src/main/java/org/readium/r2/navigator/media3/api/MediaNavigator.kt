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
interface MediaNavigator<P : MediaNavigator.Position, E : MediaNavigator.Error> : Navigator, Closeable {

    /**
     *  Marker interface for the [position] flow.
     */
    interface Position

    /**
     * Marker interface for the [Playback.error] property.
     */
    interface Error

    /**
     * State of the player.
     */
    enum class State {
        Ready,
        Buffering,
        Ended,
        Error;
    }

    /**
     * State of the playback.
     */
    data class Playback<E : Error>(
        val state: State,
        val playWhenReady: Boolean,
        val error: E?
    )

    /**
     * Indicates the current state of the playback.
     */
    val playback: StateFlow<Playback<E>>

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
