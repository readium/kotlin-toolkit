/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.syncnarr

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.api.MediaNavigatorInternal
import org.readium.r2.navigator.media3.api.SynchronizedPlayback
import org.readium.r2.navigator.media3.player.PlayerLocator
import org.readium.r2.navigator.media3.player.PlayerNavigatorInternal
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
class SynchronizedNarrationNavigatorInternal<S : Configurable.Settings, P : Configurable.Preferences<P>>(
    private val playerNavigator: PlayerNavigatorInternal<S, P>
    // media overlays data
) : MediaNavigatorInternal<PlayerLocator, SynchronizedPlayback<PlayerLocator>> {

    override val playback: StateFlow<SynchronizedPlayback<PlayerLocator>>
        get() = TODO("Not yet implemented")

    override fun play() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun go(locator: PlayerLocator) {
        TODO("Not yet implemented")
    }

    override fun goForward() {
        TODO("Not yet implemented")
    }

    override fun goBackward() {
        TODO("Not yet implemented")
    }

    override fun asPlayer(): Player {
        TODO("Not yet implemented")
    }
}
