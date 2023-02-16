/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.audio

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
class AudioNavigator<S : Configurable.Settings, P : Configurable.Preferences<P>, E : AudioEngine.Error>(
    private val mediaEngine: AudioEngine<S, P, E>
) : MediaNavigator<AudioNavigator.Position>, Configurable<S, P> by mediaEngine {

    class Position : MediaNavigator.Position

    class Error : MediaNavigator.State.Error

    override val publication: Publication
        get() = TODO("Not yet implemented")

    override val currentLocator: StateFlow<Locator>
        get() = TODO("Not yet implemented")

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override val playback: StateFlow<MediaNavigator.Playback>
        get() = TODO("Not yet implemented")

    override val position: StateFlow<Position>
        get() = TODO("Not yet implemented")

    override fun play() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun asPlayer(): Player {
        TODO("Not yet implemented")
    }
}
