/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(org.readium.r2.shared.InternalReadiumApi::class)

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
import org.readium.r2.navigator.media3.tts.session.TtsSessionAdapter
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.tokenizer.TextTokenizer

/**
 * A navigator to read aloud a [Publication] with a TTS engine.
 */
@ExperimentalReadiumApi
class TtsNavigator<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice> private constructor(
    coroutineScope: CoroutineScope,
    override val publication: Publication,
    private val player: TtsPlayer<S, P, E, V>,
    private val sessionAdapter: TtsSessionAdapter<E>,
) : SynchronizedMediaNavigator<TtsNavigator.Position>, Configurable<S, P> by player {

    companion object {

        suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
            E : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, *, E, V>,
            tokenizerFactory: (language: Language?) -> TextTokenizer,
            metadataProvider: MediaMetadataProvider,
            listener: Listener,
            initialPreferences: P? = null,
            initialLocator: Locator? = null,
        ): TtsNavigator<S, P, E, V>? {

            if (publication.findService(ContentService::class) == null) {
                return null
            }

            val actualInitialPreferences =
                initialPreferences
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
                val newPreferences = ttsEngineProvider.updatePlaybackParameters(
                    ttsPlayer.lastPreferences,
                    parameters
                )
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
        override val utteranceLocator: Locator,
        override val tokenLocator: Locator?
    ) : SynchronizedMediaNavigator.Utterance<Position>

    sealed class State {

        object Ready : MediaNavigator.State.Ready

        object Ended : MediaNavigator.State.Ended

        sealed class Error : MediaNavigator.State.Error {

            data class EngineError<E : TtsEngine.Error> (val error: E) : Error()

            data class ContentError(val exception: Exception) : Error()
        }
    }

    val voices: Set<V> get() =
        player.voices

    override val playback: StateFlow<MediaNavigator.Playback> =
        player.playback.mapStateIn(coroutineScope) { it.toPlayback() }

    override val utterance: StateFlow<Utterance> =
        player.utterance.mapStateIn(coroutineScope) { it.toUtterance() }

    override val position: StateFlow<Position> =
        utterance.mapStateIn(coroutineScope) { utterance ->
            utterance.position.copy(textAfter = utterance.text + utterance.position.textAfter)
        }

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
        utterance.mapStateIn(coroutineScope) { it.tokenLocator ?: it.utteranceLocator }

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
        )

    private fun TtsPlayer.State.toState() =
        when (this) {
            TtsPlayer.State.Ready -> State.Ready
            TtsPlayer.State.Ended -> State.Ended
            is TtsPlayer.State.Error -> this.toError()
        }

    private fun TtsPlayer.State.Error.toError(): State.Error =
        when (this) {
            is TtsPlayer.State.Error.ContentError -> State.Error.ContentError(exception)
            is TtsPlayer.State.Error.EngineError<*> -> State.Error.EngineError(error)
        }

    private fun TtsPlayer.Utterance.Position.toPosition(): Position =
        Position(
            resourceIndex = resourceIndex,
            cssSelector = cssSelector,
            textBefore = textBefore,
            textAfter = textAfter
        )

    private fun TtsPlayer.Utterance.toUtterance(): Utterance {
        val utteranceHighlight = publication
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

        val tokenHighlight = range
            ?.let { utteranceHighlight.copy(text = utteranceHighlight.text.substring(it)) }

        return Utterance(
            text = text,
            position = position.toPosition(),
            range = range,
            utteranceLocator = utteranceHighlight,
            tokenLocator = tokenHighlight,
        )
    }
}
