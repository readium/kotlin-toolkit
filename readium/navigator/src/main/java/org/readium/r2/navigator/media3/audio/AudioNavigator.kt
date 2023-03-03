/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.audio

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.navigator.extensions.time
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import timber.log.Timber

@ExperimentalReadiumApi
@OptIn(ExperimentalTime::class)
class AudioNavigator<S : Configurable.Settings, P : Configurable.Preferences<P>,
    E : AudioEngine.Error> private constructor(
    override val publication: Publication,
    private val audioEngine: AudioEngine<S, P, E>,
    private val playlist: Playlist,
    private val configuration: Configuration
) : MediaNavigator<AudioNavigator.Position>, Configurable<S, P> by audioEngine {

    companion object {

        suspend operator fun <S : Configurable.Settings, P : Configurable.Preferences<P>,
            E : AudioEngine.Error> invoke(
            publication: Publication,
            audioEngineProvider: AudioEngineProvider<S, P, *, E>,
            initialPreferences: P? = null,
            initialLocator: Locator? = null,
            configuration: Configuration = Configuration()
        ): AudioNavigator<S, P, E>? {
            val playlist = Playlist(
                publication.metadata.duration?.seconds,
                publication.readingOrder.map { Playlist.Item(it.href, it.duration?.seconds) }
            )

            val actualInitialLocator = initialLocator
                ?: publication.locatorFromLink(publication.readingOrder[0])!!

            val audioEngine =
                audioEngineProvider.createEngine(
                    publication,
                    actualInitialLocator,
                    initialPreferences ?: audioEngineProvider.createEmptyPreferences()
                ) ?: return null

            return AudioNavigator(publication, audioEngine, playlist, configuration)
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
    ) : MediaNavigator.Position {

        data class Item(
            val index: Int,
            val duration: Duration?
        )
    }

    private data class Playlist(
        val duration: Duration?,
        val items: List<Item>
    ) {
        data class Item(
            val href: String,
            val duration: Duration?
        )
    }

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
            val link = publication.readingOrder[index]
            val item = playlist.items[index]
            val itemStartPosition = playlist.items
                .slice(0 until index)
                .mapNotNull { it.duration }
                .takeIf { it.size == playlist.items.size }
                ?.sum()
            val totalProgression =
                if (itemStartPosition == null) null
                else playlist.duration?.let { (itemStartPosition + position) / it }

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
        playlist.items
            .mapNotNull { it.duration }
            .takeIf { it.size == playlist.items.size }
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
        val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
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
