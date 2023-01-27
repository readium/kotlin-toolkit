/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.media3.api.MediaMetadataProvider
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.api.SynchronizedMediaNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.publication.services.content.ContentTokenizer
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
class TtsNavigator<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice> private constructor(
    coroutineScope: CoroutineScope,
    override val publication: Publication,
    private val player: TtsPlayer<S, P, E, V>,
    private val sessionAdapter: TtsSessionAdapter<E>,
) : SynchronizedMediaNavigator<TtsNavigator.Position, TtsNavigator.Error>, Configurable<S, P> by player {

    companion object {

        suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
            E : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, *, E, V>,
            tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer,
            metadataProvider: MediaMetadataProvider,
            listener: Listener,
            initialPreferences: P? = null,
            initialLocator: Locator? = null,
        ): TtsNavigator<S, P, E, V>? {

            if (publication.findService(ContentService::class) == null) {
                return null
            }

            val actualInitialPreferences = initialPreferences
                ?: ttsEngineProvider.createEmptyPreferences()

            val contentIterator =
                TtsContentIterator(publication, tokenizerFactory, initialLocator)

            val ttsEngine =
                ttsEngineProvider.createEngine(publication, actualInitialPreferences)
                    ?: return null

            val metadataFactory =
                metadataProvider.createMetadataFactory(publication)

            val playlistMetadata =
                metadataFactory.publicationMetadata()

            val mediaItems =
                publication.readingOrder.indices.map { index ->
                    val metadata = metadataFactory.resourceMetadata(index)
                    MediaItem.Builder()
                        .setMediaMetadata(metadata)
                        .build()
                }

            val ttsPlayer =
                TtsPlayer(ttsEngine, contentIterator, actualInitialPreferences)
                    ?: return null

            val coroutineScope =
                MainScope()

            val playbackParameters =
                ttsPlayer.settings.mapStateIn(coroutineScope) {
                    ttsEngineProvider.getPlaybackParameters(it)
                }

            val onSetPlaybackParameters = { parameters: PlaybackParameters ->
                val newPreferences = ttsEngineProvider.updatePlaybackParameters(ttsPlayer.lastPreferences, parameters)
                ttsPlayer.submitPreferences(newPreferences)
            }

            val sessionAdapter =
                TtsSessionAdapter(
                    application,
                    ttsPlayer,
                    playlistMetadata,
                    mediaItems,
                    listener::onStopRequested,
                    playbackParameters,
                    onSetPlaybackParameters,
                    ttsEngineProvider::mapEngineError
                )

            return TtsNavigator(coroutineScope, publication, ttsPlayer, sessionAdapter)
        }
    }

    interface Listener {

        fun onStopRequested()

        fun onMissingLanguageData()
    }

    data class Position(
        val resourceIndex: Int,
        val cssSelector: String,
        val textBefore: String?,
        val textAfter: String?,
    ) : MediaNavigator.Position


    data class Utterance(
        override val text: String,
        override val position: Position,
        override val range: IntRange?,
        override val locator: Locator
    ) : SynchronizedMediaNavigator.Utterance<Position> {

        override val rangeLocator: Locator? = range
            ?.let { locator.copy(text = locator.text.substring(it)) }
    }

    sealed class Error : MediaNavigator.Error {

        data class EngineError<E : TtsEngine.Error> (val error: E) : Error()

        data class ContentError(val exception: Exception) : Error()
    }

    val voices: Set<V> get() =
        player.voices

    override val playback: StateFlow<MediaNavigator.Playback<Error>> =
        player.playback.mapStateIn(coroutineScope) { it.toPlayback() }

    override val utterance: StateFlow<Utterance> =
        player.utterance.mapStateIn(coroutineScope) { it.toUtterance() }

    override val position: StateFlow<Position> =
        utterance.mapStateIn(coroutineScope) { it.position }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    fun go(locator: Locator) {
        player.go(locator)
    }

    fun previousUtterance() {
        player.previousUtterance()
    }

    fun nextUtterance() {
        player.nextUtterance()
    }

    override fun asPlayer(): Player =
        sessionAdapter

    override fun close() {
        player.close()
    }

    override val currentLocator: StateFlow<Locator> =
        utterance.mapStateIn(coroutineScope) { it.locator }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        player.go(locator)
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator, animated, completion)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        player.nextUtterance()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        player.previousUtterance()
        return true
    }

    private fun TtsPlayer.Playback.toPlayback() =
        MediaNavigator.Playback(
            state = state.toState(),
            playWhenReady = playWhenReady,
            error = error?.toError()
        )

    private fun TtsPlayer.Playback.State.toState() =
        when (this) {
            TtsPlayer.Playback.State.Ready -> MediaNavigator.State.Ready
            TtsPlayer.Playback.State.Ended -> MediaNavigator.State.Ended
            TtsPlayer.Playback.State.Error -> MediaNavigator.State.Error
        }

    private fun TtsPlayer.Error.toError(): Error =
        when (this) {
            is TtsPlayer.Error.ContentError -> Error.ContentError(exception)
            is TtsPlayer.Error.EngineError<*> -> Error.EngineError(error)
        }

    private fun TtsPlayer.Utterance.toUtterance(): Utterance {
        val position = Position(
            resourceIndex = position.resourceIndex,
            cssSelector = position.cssSelector,
            textBefore = position.textBefore,
            textAfter = position.textAfter
        )

        val locator = publication
            .locatorFromLink(publication.readingOrder[position.resourceIndex])!!
            .copyWithLocations(
                progression = null,
                otherLocations = buildMap {
                    put("cssSelector", position.cssSelector)
                }
            ).copy(
                text =
                Locator.Text(
                    highlight = text,
                    before = position.textBefore,
                    after = position.textAfter
                )
            )

        return Utterance(
            text = text,
            position = position,
            locator = locator,
            range = range
        )
    }
}
