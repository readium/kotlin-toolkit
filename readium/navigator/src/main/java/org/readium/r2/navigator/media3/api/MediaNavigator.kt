/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Closeable

@ExperimentalReadiumApi
interface MediaNavigator<P : MediaNavigator.Playback> : Navigator, Closeable {

    enum class State {
        Playing,
        Paused,
        Ended;
    }

    data class Buffer(
        val isPlayable: Boolean,
        val position: Duration
    )

    interface Playback {

        val state: State
        val locator: Locator
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
     * Adapts this navigator to the media3 [Player] interface.
     */
    fun asPlayer(): Player
}
