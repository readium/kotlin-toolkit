/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
interface MediaNavigatorInternal<P : MediaNavigatorInternal.Position,
    R : MediaNavigatorInternal.RelaxedPosition, E : MediaNavigatorInternal.Error> {

    interface Position

    interface RelaxedPosition

    interface Error

    enum class State {
        Ready,
        Buffering,
        Ended,
        Error;
    }

    data class Playback<E : Error>(
        val state: State,
        val playWhenReady: Boolean,
        val error: E?
    )

    val playback: StateFlow<Playback<E>>

    val progression: StateFlow<P>

    /**
     * Resumes the playback at the current location.
     */
    fun play()

    /**
     * Pauses the playback.
     */
    fun pause()

    /**
     * Seeks to the given locator.
     */
    fun go(position: R)

    /**
     * Skips forward
     */
    fun goForward()

    /**
     * Skips backward.
     */
    fun goBackward()

    /**
     * Adapts this navigator to the media3 [Player] interface.
     */
    fun asPlayer(): Player
}
