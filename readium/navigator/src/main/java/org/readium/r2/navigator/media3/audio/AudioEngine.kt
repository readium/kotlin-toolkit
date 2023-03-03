/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.audio

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
interface AudioEngine<S : Configurable.Settings, P : Configurable.Preferences<P>, E : AudioEngine.Error> :
    Configurable<S, P> {

    interface Error

    data class Playback<E : Error>(
        val state: MediaNavigator.State,
        val playWhenReady: Boolean,
        val error: E?
    )

    data class Position(
        val index: Int,
        val duration: Duration
    )

    val playback: StateFlow<Playback<E>>

    val position: StateFlow<Position>

    fun play()

    fun pause()

    fun seek(index: Long, position: Duration)

    fun close()

    fun asPlayer(): Player
}
