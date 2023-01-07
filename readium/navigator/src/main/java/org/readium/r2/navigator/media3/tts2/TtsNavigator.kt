/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.media3.api.MediaNavigator
import org.readium.r2.navigator.media3.api.MediaNavigatorInternal
import org.readium.r2.navigator.media3.api.MetadataProvider
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
class TtsNavigator<S : TtsSettings, P : TtsPreferences<P>> private constructor(
    override val publication: Publication,
    private val ttsNavigator: TtsNavigatorInternal<S, P>
) : MediaNavigator<TtsNavigator.Playback>, Navigator, Configurable<S, P> by ttsNavigator {

    companion object {

        suspend operator fun <S : TtsSettings, P : TtsPreferences<P>> invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, *>,
            tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer,
            metadataProvider: MetadataProvider,
            listener: TtsNavigatorListener,
            initialPreferences: P? = null,
            initialLocator: Locator? = null,
        ): TtsNavigator<S, P>? {

            if (publication.findService(ContentService::class) == null) {
                return null
            }

            val actualInitialLocator = initialLocator
                ?.toTtsLocator(publication)

            val actualInitialPreferences = initialPreferences
                ?: ttsEngineProvider.createEmptyPreferences()

            val contentIterator =
                TtsContentIterator(publication, tokenizerFactory, actualInitialLocator)

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
                TtsNavigatorInternal(application, ttsEngine, contentIterator, playlistMetadata, mediaItems, listener)
                    ?: return null

            return TtsNavigator(publication, internalNavigator)
        }
    }

    data class Playback(
        override val state: MediaNavigator.State,
        override val locator: Locator,
        override val token: Locator?
    ) : MediaNavigator.Playback, MediaNavigator.TextSynchronization

    private val coroutineScope: CoroutineScope =
        MainScope()

    override val playback: StateFlow<Playback> =
        ttsNavigator.playback.mapStateIn(coroutineScope) { it.toPlayback() }

    private fun TtsPlayback.toPlayback() =
        Playback(
            state = when (state) {
                MediaNavigatorInternal.State.Playing -> MediaNavigator.State.Playing
                MediaNavigatorInternal.State.Paused -> MediaNavigator.State.Paused
                MediaNavigatorInternal.State.Ended -> MediaNavigator.State.Ended
            },
            locator = locator.toLocator(publication),
            token = token?.toLocator(publication)
        )

    override val currentLocator: StateFlow<Locator> =
        playback.mapStateIn(coroutineScope) { it.locator }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        val ttsLocator = locator.toTtsLocator(publication) ?: return false
        ttsNavigator.go(ttsLocator)
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
}
