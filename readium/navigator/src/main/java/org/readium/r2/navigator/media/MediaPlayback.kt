/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import kotlin.time.Duration
import org.readium.r2.shared.InternalReadiumApi

/**
 * State of the playback at a point in time.
 *
 * @param state State of the playback.
 * @param rate Speed of the playback, defaults to 1.0.
 * @param timeline Position and duration of the current resource.
 */
@InternalReadiumApi
public data class MediaPlayback(val state: State, val rate: Double, val timeline: Timeline) {

    public enum class State {
        Idle, Loading, Playing, Paused;

        public val isPlaying: Boolean get() =
            (this == Playing || this == Loading)
    }

    public data class Timeline(
        val position: Duration,
        val duration: Duration?,
        val buffered: Duration?
    )

    val isPlaying: Boolean get() = state.isPlaying
}
