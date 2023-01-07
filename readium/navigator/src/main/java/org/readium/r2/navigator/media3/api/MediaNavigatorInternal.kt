/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
interface MediaNavigatorInternal<L : MediaNavigatorInternal.Locator, P : MediaNavigatorInternal.Playback<L>> {

    interface Locator

    enum class State {
        Playing,
        Paused,
        Ended;
    }

    data class Buffer(
        val isPlayable: Boolean,
        val position: Duration
    )

    interface Playback<L : Locator> {

        val state: State
        val locator: L
    }

    interface TextSynchronization {

        val token: Locator?
    }

    interface BufferProvider {

        val buffer: Buffer
    }

    val playback: StateFlow<P>

    /**
     * Resumes the playback at the current location or start it again from the beginning if it has finished.
     */
    fun play()

    /**
     * Pauses the playback.
     */
    fun pause()

    /**
     * Seeks to the given locator.
     */
    fun go(locator: L)

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
