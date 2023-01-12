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
interface MediaNavigator<E : MediaNavigator.Error> : Navigator, Closeable {

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

    /**
     * Resumes the playback at the current location or start it again from the beginning if it has finished.
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
