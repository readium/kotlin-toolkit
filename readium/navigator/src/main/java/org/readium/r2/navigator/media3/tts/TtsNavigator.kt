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
import org.readium.r2.navigator.extensions.normalizeLocator
import org.readium.r2.navigator.media3.api.Media3Adapter
import org.readium.r2.navigator.media3.api.MediaMetadataProvider
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.api.TextAwareMediaNavigator
import org.readium.r2.navigator.media3.tts.session.TtsSessionAdapter
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.combineStateIn
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.UrlHref
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.tokenizer.TextTokenizer

/**
 * A navigator to read aloud a [Publication] with a TTS engine.
 */
@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
public class TtsNavigator<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice> private constructor(
    coroutineScope: CoroutineScope,
    override val publication: Publication,
    private val player: TtsPlayer<S, P, E, V>,
    private val sessionAdapter: TtsSessionAdapter<E>
) :
    MediaNavigator<TtsNavigator.Location, TtsNavigator.Playback, TtsNavigator.ReadingOrder>,
    TextAwareMediaNavigator<TtsNavigator.Location, TtsNavigator.Playback, TtsNavigator.ReadingOrder>,
    Media3Adapter,
    Configurable<S, P> {

    public companion object {

        public suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
            E : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, *, E, V>,
            tokenizerFactory: (language: Language?) -> TextTokenizer,
            metadataProvider: MediaMetadataProvider,
            listener: Listener,
            initialLocator: Locator? = null,
            initialPreferences: P? = null
        ): TtsNavigator<S, P, E, V>? {
            if (publication.findService(ContentService::class) == null) {
                return null
            }

            @Suppress("NAME_SHADOWING")
            val initialLocator =
                initialLocator?.let { publication.normalizeLocator(it) }

            val actualInitialPreferences =
                initialPreferences
                    ?: ttsEngineProvider.createEmptyPreferences()

            val contentIterator =
                TtsUtteranceIterator(publication, tokenizerFactory, initialLocator)

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

    public interface Listener {

        public fun onStopRequested()
    }

    public data class Location(
        override val href: Url,
        override val utterance: String,
        override val range: IntRange?,
        override val textBefore: String?,
        override val textAfter: String?,
        override val utteranceLocator: Locator,
        override val tokenLocator: Locator?
    ) : TextAwareMediaNavigator.Location

    public data class Playback(
        override val state: MediaNavigator.State,
        override val playWhenReady: Boolean,
        override val index: Int,
        override val utterance: String,
        override val range: IntRange?
    ) : TextAwareMediaNavigator.Playback

    public data class ReadingOrder(
        override val items: List<Item>
    ) : TextAwareMediaNavigator.ReadingOrder {

        public data class Item(
            val href: Url
        ) : TextAwareMediaNavigator.ReadingOrder.Item
    }

    public sealed class State {

        public object Ready : MediaNavigator.State.Ready

        public object Ended : MediaNavigator.State.Ended

        public sealed class Error : MediaNavigator.State.Error {

            public data class EngineError<E : TtsEngine.Error> (val error: E) : Error()

            public data class ContentError(val exception: Exception) : Error()
        }
    }

    public val voices: Set<V> get() =
        player.voices

    override val readingOrder: ReadingOrder =
        ReadingOrder(
            items = publication.readingOrder.mapNotNull {
                ReadingOrder.Item(
                    href = it.href() ?: return@mapNotNull null
                )
            }
        )

    override val playback: StateFlow<Playback> =
        player.playback.combineStateIn(coroutineScope, player.utterance) { playback, utterance ->
            navigatorPlayback(playback, utterance)
        }

    override val location: StateFlow<Location> =
        player.utterance.mapStateIn(coroutineScope) { playerUtterance ->
            playerUtterance.toPosition()
        }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    public fun go(locator: Locator) {
        player.go(publication.normalizeLocator(locator))
    }

    override fun goToPreviousUtterance() {
        player.previousUtterance()
    }

    override fun goToNextUtterance() {
        player.nextUtterance()
    }

    override fun hasPreviousUtterance(): Boolean {
        return player.hasPreviousUtterance()
    }

    override fun hasNextUtterance(): Boolean {
        return player.hasNextUtterance()
    }

    override fun asMedia3Player(): Player =
        sessionAdapter

    override fun close() {
        player.close()
    }

    override val currentLocator: StateFlow<Locator> =
        location.mapStateIn(coroutineScope) { it.tokenLocator ?: it.utteranceLocator }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        player.go(publication.normalizeLocator(locator))
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

    override val settings: StateFlow<S> =
        player.settings

    override fun submitPreferences(preferences: P) {
        player.submitPreferences(preferences)
        player.restartUtterance()
    }

    private fun navigatorPlayback(playback: TtsPlayer.Playback, utterance: TtsPlayer.Utterance) =
        Playback(
            state = playback.state.toState(),
            playWhenReady = playback.playWhenReady,
            index = utterance.position.resourceIndex,
            utterance = utterance.text,
            range = utterance.range
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

    private fun TtsPlayer.Utterance.toPosition(): Location {
        val currentLink = publication.readingOrder[position.resourceIndex]
        val url = (currentLink.href as UrlHref).url

        val utteranceLocator = publication
            .locatorFromLink(currentLink)!!
            .copy(
                locations = position.locations,
                text = position.text
            )

        val tokenLocator = range
            ?.let { utteranceLocator.copy(text = utteranceLocator.text.substring(it)) }

        return Location(
            href = url,
            textBefore = position.text.before,
            textAfter = position.text.after,
            utterance = text,
            range = range,
            utteranceLocator = utteranceLocator,
            tokenLocator = tokenLocator
        )
    }
}
