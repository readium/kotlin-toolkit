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
class AudioBookNavigator<S : Configurable.Settings, P : Configurable.Preferences<P>> private constructor(
    override val publication: Publication,
    private val audioEngine: AudioEngine<S, P>,
    override val readingOrder: ReadingOrder,
    private val configuration: Configuration
) : AudioNavigator<AudioBookNavigator.Location, AudioBookNavigator.Playback, AudioBookNavigator.ReadingOrder>,
    Configurable<S, P> by audioEngine {

    companion object {

        suspend operator fun <S : Configurable.Settings, P : Configurable.Preferences<P>> invoke(
            publication: Publication,
            audioEngineProvider: AudioEngineProvider<S, P, *>,
            readingOrder: List<Link> = publication.readingOrder,
            initialPreferences: P? = null,
            initialLocator: Locator? = null,
            configuration: Configuration = Configuration()
        ): AudioBookNavigator<S, P>? {
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

    data class Location(
        override val href: Href,
        override val offset: Duration,
    ) : AudioNavigator.Location

    data class ReadingOrder(
        override val duration: Duration?,
        override val items: List<Item>
    ) : AudioNavigator.ReadingOrder {

        data class Item(
            val href: Href,
            override val duration: Duration?
        ) : AudioNavigator.ReadingOrder.Item
    }

    data class Playback(
        override val state: MediaNavigator.State,
        override val playWhenReady: Boolean,
        override val index: Int,
        override val offset: Duration,
        override val buffered: Duration?,
    ) : AudioNavigator.Playback

    sealed class State {

        object Ready : MediaNavigator.State.Ready

        object Ended : MediaNavigator.State.Ended

        object Buffering : MediaNavigator.State.Buffering

        data class Error<E : AudioEngine.Error> (val error: E) : MediaNavigator.State.Error
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    override val currentLocator: StateFlow<Locator> =
        audioEngine.playback.mapStateIn(coroutineScope) { playback ->
            val currentItem = readingOrder.items[playback.index]
            val link = requireNotNull(publication.linkWithHref(currentItem.href.string))
            val item = readingOrder.items[playback.index]
            val itemStartPosition = readingOrder.items
                .slice(0 until playback.index)
                .mapNotNull { it.duration }
                .takeIf { it.size == readingOrder.items.size }
                ?.sum()
            val totalProgression =
                if (itemStartPosition == null) null
                else readingOrder.duration?.let { (itemStartPosition + playback.offset) / it }

            val locator = requireNotNull(publication.locatorFromLink(link))
            locator.copyWithLocations(
                fragments = listOf("t=${playback.offset.inWholeSeconds}"),
                progression = item.duration?.let { playback.offset / it },
                totalProgression = totalProgression
            )
        }

    override val playback: StateFlow<Playback> =
        audioEngine.playback.mapStateIn(coroutineScope) { playback ->
            Playback(
                playback.state.toState(),
                playback.playWhenReady,
                playback.index,
                playback.offset,
                playback.buffered
            )
        }

    override val location: StateFlow<Location> =
        audioEngine.playback.mapStateIn(coroutineScope) {
            val currentItem = readingOrder.items[it.index]
            Location(currentItem.href, it.offset)
        }

    override fun play() {
        audioEngine.play()
    }

    override fun pause() {
        audioEngine.pause()
    }

    override fun seek(index: Int, offset: Duration) {
        audioEngine.seek(index, offset)
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
                audioEngine.playback.value.offset,
                audioEngine.playback.value.index,
                durations
            )
        Timber.v("Smart seeking by $offset resolved to item $newIndex position $newPosition")
        audioEngine.seek(newIndex, newPosition)
    }

    private fun dumbSeekBy(offset: Duration) {
        val newIndex = audioEngine.playback.value.index
        val newPosition = audioEngine.playback.value.offset + offset
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

    private fun AudioEngine.State.toState(): MediaNavigator.State =
        when (this) {
            is AudioEngine.State.Ready -> State.Ready
            is AudioEngine.State.Ended -> State.Ended
            is AudioEngine.State.Buffering -> State.Buffering
            is AudioEngine.State.Error -> State.Error(error)
        }
}
