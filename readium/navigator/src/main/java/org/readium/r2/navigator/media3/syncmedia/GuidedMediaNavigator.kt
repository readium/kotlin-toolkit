/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.syncmedia

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.api.Media3Adapter
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.api.TextAwareMediaNavigator
import org.readium.r2.navigator.media3.api.TimeBasedMediaNavigator
import org.readium.r2.navigator.media3.audio.AudioNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public class GuidedMediaNavigator<S : Configurable.Settings, P : Configurable.Preferences<P>>(
    private val audioNavigator: AudioNavigator<S, P>
) :
    MediaNavigator<GuidedMediaNavigator.Location, GuidedMediaNavigator.Playback, GuidedMediaNavigator.ReadingOrder>,
    TimeBasedMediaNavigator<GuidedMediaNavigator.Location, GuidedMediaNavigator.Playback, GuidedMediaNavigator.ReadingOrder>,
    TextAwareMediaNavigator<GuidedMediaNavigator.Location, GuidedMediaNavigator.Playback, GuidedMediaNavigator.ReadingOrder>,
    Media3Adapter,
    Configurable<S, P> {

    public data class Location(
        override val href: Url,
        override val offset: Duration,
        val fragment: String,
        override val textBefore: String?,
        override val textAfter: String?,
        override val utterance: String,
        override val range: IntRange?,
        override val utteranceLocator: Locator,
        override val tokenLocator: Locator?
    ) : TimeBasedMediaNavigator.Location,
        TextAwareMediaNavigator.Location

    public data class Playback(
        override val state: MediaNavigator.State,
        override val playWhenReady: Boolean,
        override val index: Int,
        override val offset: Duration,
        override val buffered: Duration?,
        override val utterance: String,
        override val range: IntRange?
    ) : TimeBasedMediaNavigator.Playback, TextAwareMediaNavigator.Playback

    public data class ReadingOrder(
        override val duration: Duration?,
        override val items: List<Item>
    ) : TimeBasedMediaNavigator.ReadingOrder, TextAwareMediaNavigator.ReadingOrder {

        public data class Item(
            val href: Href,
            override val duration: Duration?
        ) : TimeBasedMediaNavigator.ReadingOrder.Item, TextAwareMediaNavigator.ReadingOrder.Item
    }

    override val publication: Publication =
        audioNavigator.publication

    override val currentLocator: StateFlow<Locator> =
        audioNavigator.currentLocator

    override val playback: StateFlow<Playback>
        get() = TODO("Not yet implemented")

    override val location: StateFlow<Location>
        get() = TODO("Not yet implemented")

    override val readingOrder: ReadingOrder
        get() = TODO("Not yet implemented")

    override fun play() {
        audioNavigator.play()
    }

    override fun pause() {
        audioNavigator.pause()
    }

    override fun asMedia3Player(): Player =
        audioNavigator.asMedia3Player()

    override val settings: StateFlow<S> =
        audioNavigator.settings

    override fun submitPreferences(preferences: P) {
        audioNavigator.submitPreferences(preferences)
    }

    override fun close() {
        audioNavigator.close()
    }

    override fun goToPreviousUtterance() {
        TODO("Not yet implemented")
    }

    override fun goToNextUtterance() {
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

    override fun seek(index: Int, offset: Duration) {
        TODO("Not yet implemented")
    }
}
