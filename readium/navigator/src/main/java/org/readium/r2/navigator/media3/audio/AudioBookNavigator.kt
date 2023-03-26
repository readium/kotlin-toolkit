/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.audio

import android.os.Build
import androidx.media3.common.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.navigator.extensions.time
import org.readium.r2.navigator.media3.api.AudioNavigator
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Href
import timber.log.Timber

@ExperimentalReadiumApi
@OptIn(ExperimentalTime::class)
class AudioBookNavigator<S : Configurable.Settings, P : Configurable.Preferences<P>,
    E : AudioEngine.Error> private constructor(
    override val publication: Publication,
    private val audioEngine: AudioEngine<S, P, E>,
    override val readingOrder: ReadingOrder,
    private val configuration: Configuration
) : AudioNavigator<AudioBookNavigator.Position>, Configurable<S, P> by audioEngine {

    companion object {

        suspend operator fun <S : Configurable.Settings, P : Configurable.Preferences<P>,
            E : AudioEngine.Error> invoke(
            publication: Publication,
            audioEngineProvider: AudioEngineProvider<S, P, *, E>,
            readingOrder: List<Link> = publication.readingOrder,
            initialPreferences: P? = null,
            initialLocator: Locator? = null,
            configuration: Configuration = Configuration()
        ): AudioBookNavigator<S, P, E>? {
            val items = readingOrder.map { ReadingOrder.Item(Href(it.href), duration(it, publication)) }
            val totalDuration = publication.metadata.duration?.seconds
                ?: items.mapNotNull { it.duration }
                    .takeIf { it.size == items.size }
                    ?.sum()

            val actualReadingOrder = ReadingOrder(totalDuration, items)

            val actualInitialLocator = initialLocator
                ?: publication.locatorFromLink(publication.readingOrder[0])!!

            val audioEngine =
                audioEngineProvider.createEngine(
                    publication,
                    actualInitialLocator,
                    initialPreferences ?: audioEngineProvider.createEmptyPreferences()
                ) ?: return null

            return AudioBookNavigator(publication, audioEngine, actualReadingOrder, configuration)
        }

        private fun duration(link: Link, publication: Publication): Duration? {
            var duration: Duration? = link.duration?.seconds

            if (duration == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val resource = publication.get(link)
                duration = MetadataRetriever(resource).duration()
            }

            return duration
        }
    }

    data class Configuration(
        val positionRefreshRate: Double = 2.0, // Hz
        val skipForwardInterval: Duration = 30.seconds,
        val skipBackwardInterval: Duration = 30.seconds,
    )

    data class Position(
        val item: Item,
        val offset: Duration,
        val buffered: Duration?
    ) : AudioNavigator.Position {

        data class Item(
            val index: Int,
            val duration: Duration?
        )
    }

    data class ReadingOrder(
        override val duration: Duration?,
        override val items: List<Item>
    ) : AudioNavigator.ReadingOrder {

        data class Item(
            override val href: Href,
            override val duration: Duration?
        ) : AudioNavigator.ReadingOrder.Item
    }

    data class Resource(
        override val index: Int,
        override val position: Duration,
        override val buffered: Duration?,
    ) : AudioNavigator.Resource

    sealed class State {

        object Ready : MediaNavigator.State.Ready

        object Ended : MediaNavigator.State.Ended

        object Buffering : MediaNavigator.State.Buffering

        class Error : MediaNavigator.State.Error
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    override val currentLocator: StateFlow<Locator> =
        audioEngine.position.mapStateIn(coroutineScope) { (index, position) ->
            val link = requireNotNull(publication.linkWithHref(readingOrder.items[index].href.string))
            val item = readingOrder.items[index]
            val itemStartPosition = readingOrder.items
                .slice(0 until index)
                .mapNotNull { it.duration }
                .takeIf { it.size == readingOrder.items.size }
                ?.sum()
            val totalProgression =
                if (itemStartPosition == null) null
                else readingOrder.duration?.let { (itemStartPosition + position) / it }

            val locator = requireNotNull(publication.locatorFromLink(link))
            locator.copyWithLocations(
                fragments = listOf("t=${position.inWholeSeconds}"),
                progression = item.duration?.let { position / it },
                totalProgression = totalProgression
            )
        }

    override val playback: StateFlow<MediaNavigator.Playback> =
        audioEngine.playback.mapStateIn(coroutineScope) {
            MediaNavigator.Playback(it.state, it.playWhenReady)
        }

    override val position: StateFlow<Position> =
        audioEngine.position.mapStateIn(coroutineScope) {
            Position(Position.Item(it.index, it.duration), it.position, it.buffered)
        }

    override val resource: StateFlow<Resource> =
        audioEngine.position.mapStateIn(coroutineScope) {
            Resource(it.index, it.position, it.buffered)
        }

    override fun play() {
        audioEngine.play()
    }

    override fun pause() {
        audioEngine.pause()
    }

    fun seek(index: Int, position: Duration) {
        audioEngine.seek(index, position)
    }

    fun seekForward() {
        seekBy(configuration.skipForwardInterval)
    }

    fun seekBackward() {
        seekBy(-configuration.skipBackwardInterval)
    }

    private fun seekBy(offset: Duration) {
        readingOrder.items
            .mapNotNull { it.duration }
            .takeIf { it.size == readingOrder.items.size }
            ?.let { smartSeekBy(offset, it) }
            ?: dumbSeekBy(offset)
    }

    private fun smartSeekBy(
        offset: Duration,
        durations: List<Duration>
    ) {
        val (newIndex, newPosition) =
            SmartSeeker.dispatchSeek(
                offset,
                audioEngine.position.value.position,
                audioEngine.position.value.index,
                durations
            )
        Timber.v("Smart seeking by $offset resolved to item $newIndex position $newPosition")
        audioEngine.seek(newIndex, newPosition)
    }

    private fun dumbSeekBy(offset: Duration) {
        val newIndex = audioEngine.position.value.index
        val newPosition = audioEngine.position.value.position + offset
        audioEngine.seek(newIndex, newPosition)
    }

    override fun close() {
        audioEngine.close()
    }

    override fun asPlayer(): Player {
        return audioEngine.asPlayer()
    }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        val itemIndex = readingOrder.items.indexOfFirst { it.href.string == locator.href }
            .takeUnless { it == -1 }
            ?: return false
        val position = locator.locations.time ?: Duration.ZERO
        Timber.v("Go to locator $locator")
        audioEngine.seek(itemIndex, position)
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        seekForward()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        seekBackward()
        return true
    }
}
