/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.syncmedia

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.api.AudioNavigator
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.api.SynchronizedMediaNavigator
import org.readium.r2.navigator.media3.audio.AudioBookNavigator
import org.readium.r2.navigator.media3.audio.AudioEngine
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Href

@ExperimentalReadiumApi
class SyncAudioNavigator<S : Configurable.Settings, P : Configurable.Preferences<P>, E : AudioEngine.Error>(
    private val audioNavigator: AudioBookNavigator<S, P, E>,
) : AudioNavigator<SyncAudioNavigator.Position>,
    SynchronizedMediaNavigator<SyncAudioNavigator.Position>,
    Configurable<S, P> {

    data class Position(
        val resourceIndex: Int,
        val fragment: String,
        override val textBefore: String?,
        override val textAfter: String?,
        override val text: String,
        override val range: IntRange?,
        override val utteranceLocator: Locator,
        override val tokenLocator: Locator?,
    ) : AudioNavigator.Position, SynchronizedMediaNavigator.Position

    data class Playback(
        override val state: MediaNavigator.State,
        override val playWhenReady: Boolean,
        override val index: Int,
        override val offset: Duration,
        override val buffered: Duration?,
        override val utterance: String,
        override val range: IntRange?,
    ) : AudioNavigator.Playback, SynchronizedMediaNavigator.Playback

    data class ReadingOrder(
        override val duration: Duration?,
        override val items: List<Item>
    ) : AudioNavigator.ReadingOrder, SynchronizedMediaNavigator.ReadingOrder {

        data class Item(
            override val href: Href,
            override val duration: Duration?
        ) : AudioNavigator.ReadingOrder.Item, SynchronizedMediaNavigator.ReadingOrder.Item
    }

    override val publication: Publication =
        audioNavigator.publication

    override val currentLocator: StateFlow<Locator> =
        audioNavigator.currentLocator

    override val playback: StateFlow<MediaNavigator.Playback> =
        audioNavigator.playback

    override val position: StateFlow<Position>
        get() = TODO("Not yet implemented")

    override val readingOrder: ReadingOrder
        get() = TODO("Not yet implemented")

    override fun play() {
        audioNavigator.play()
    }

    override fun pause() {
        audioNavigator.pause()
    }

    override fun asPlayer(): Player {
        return audioNavigator.asPlayer()
    }

    override val settings: StateFlow<S> =
        audioNavigator.settings

    override fun submitPreferences(preferences: P) {
        audioNavigator.submitPreferences(preferences)
    }

    override fun close() {
        audioNavigator.close()
    }

    override fun previousUtterance() {
        TODO("Not yet implemented")
    }

    override fun nextUtterance() {
        TODO("Not yet implemented")
    }

    override fun hasPreviousUtterance(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasNextUtterance(): Boolean {
        TODO("Not yet implemented")
    }

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
}
