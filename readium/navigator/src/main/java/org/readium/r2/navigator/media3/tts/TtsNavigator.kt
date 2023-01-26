/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.media3.api.*
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.publication.services.content.ContentTokenizer
import org.readium.r2.shared.util.Language
import timber.log.Timber

@ExperimentalReadiumApi
class TtsNavigator<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice> private constructor(
    override val publication: Publication,
    private val ttsNavigator: TtsNavigatorInternal<S, P, E, V>
) : SynchronizedMediaNavigator<TtsNavigator.Error>, Navigator, Configurable<S, P> by ttsNavigator {

    companion object {

        suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
            E : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, *, E, V>,
            tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer,
            metadataProvider: MediaMetadataProvider,
            listener: TtsNavigatorListener,
            initialPreferences: P? = null,
            initialLocator: Locator? = null,
        ): TtsNavigator<S, P, E, V>? {

            if (publication.findService(ContentService::class) == null) {
                return null
            }

            Timber.d("initialLocator $initialLocator")

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

            val internalNavigator =
                TtsNavigatorInternal(
                    application,
                    ttsEngine,
                    contentIterator,
                    playlistMetadata,
                    mediaItems,
                    ttsEngineProvider::getPlaybackParameters,
                    ttsEngineProvider::updatePlaybackParameters,
                    ttsEngineProvider::mapEngineError,
                    actualInitialPreferences,
                    listener,
                ) ?: return null

            return TtsNavigator(publication, internalNavigator)
        }
    }

    sealed class Error : MediaNavigator.Error {

        data class EngineError<E : TtsEngine.Error> (val error: E) : Error()

        data class ContentError(val exception: Exception) : Error()
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    override val playback: StateFlow<MediaNavigator.Playback<Error>> =
        ttsNavigator.playback.mapStateIn(coroutineScope) { it.toPlayback() }

    override val utterance: StateFlow<SynchronizedMediaNavigator.Utterance> =
        ttsNavigator.utterance.mapStateIn(coroutineScope) { Timber.d("utterance $it"); it.toUtterance() }

    override val currentLocator: StateFlow<Locator> =
        ttsNavigator.utterance.mapStateIn(coroutineScope) { it.toLocator() }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        ttsNavigator.go(TtsNavigatorInternal.RelaxedPosition(locator))
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        ttsNavigator.goForward()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        ttsNavigator.goBackward()
        return true
    }

    override fun close() {
        ttsNavigator.close()
    }

    override fun play() {
        ttsNavigator.play()
    }

    override fun pause() {
        ttsNavigator.pause()
    }

    override fun asPlayer(): Player =
        ttsNavigator.asPlayer()

    private fun MediaNavigatorInternal.Playback<TtsNavigatorInternal.Error>.toPlayback() =
        MediaNavigator.Playback(
            state = state.toState(),
            playWhenReady = playWhenReady,
            error = error?.toError()
        )

    private fun MediaNavigatorInternal.State.toState() =
        when (this) {
            MediaNavigatorInternal.State.Ready -> MediaNavigator.State.Ready
            MediaNavigatorInternal.State.Ended -> MediaNavigator.State.Ended
            MediaNavigatorInternal.State.Buffering -> MediaNavigator.State.Buffering
            MediaNavigatorInternal.State.Error -> MediaNavigator.State.Error
        }

    private fun TtsNavigatorInternal.Error.toError(): Error =
        when (this) {
            is TtsNavigatorInternal.Error.ContentError -> Error.ContentError(exception)
            is TtsNavigatorInternal.Error.EngineError<*> -> Error.EngineError(error)
        }

    private fun SynchronizedMediaNavigatorInternal.Utterance<TtsNavigatorInternal.Position>.toUtterance() =
        SynchronizedMediaNavigator.Utterance(
            locator = toLocator(),
            range = range
        )

    private fun SynchronizedMediaNavigatorInternal.Utterance<TtsNavigatorInternal.Position>.toLocator() =
        publication
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
}
