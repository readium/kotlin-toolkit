/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import androidx.media3.common.Player
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
internal data class TtsEngineFacadePlayback(
    val state: State,
    val isPlaying: Boolean,
    val playWhenReady: Boolean,
    val index: Int,
    val locator: TtsLocator,
    val range: IntRange?
) {

    enum class State(val value: Int) {
        READY(Player.STATE_READY),
        ENDED(Player.STATE_ENDED);
    }
}
