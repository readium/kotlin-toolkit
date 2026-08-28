/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.tts

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import kotlinx.coroutines.MainScope
import org.readium.navigator.media.common.DefaultMediaMetadataProvider
import org.readium.navigator.media.common.MediaMetadataProvider
import org.readium.navigator.media.tts.android.AndroidTtsDefaults
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsEngineProvider
import org.readium.navigator.media.tts.session.TtsSessionAdapter
import org.readium.r2.navigator.extensions.normalizeLocator
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.mapStateIn
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.publication.services.content.content
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.tokenizer.DefaultTextContentTokenizer
import org.readium.r2.shared.util.tokenizer.TextTokenizer
import org.readium.r2.shared.util.tokenizer.TextUnit

@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
public class TtsNavigatorFactory<
    S : TtsEngine.Settings,
    P : TtsEngine.Preferences<P>,
    E : PreferencesEditor<P>,
    F : TtsEngine.Error,
    V : TtsEngine.Voice,
    > private constructor(
    private val application: Application,
    private val publication: Publication,
    private val ttsEngineProvider: TtsEngineProvider<S, P, E, F, V>,
    private val tokenizerFactory: (language: Language?) -> TextTokenizer,
    private val metadataProvider: MediaMetadataProvider,
) {
    public companion object {

        public operator fun invoke(
            application: Application,
            publication: Publication,
            tokenizerFactory: (language: Language?) -> TextTokenizer = defaultTokenizerFactory,
            metadataProvider: MediaMetadataProvider = defaultMediaMetadataProvider,
            defaults: AndroidTtsDefaults = AndroidTtsDefaults(),
            voiceSelector: (Language?, Set<AndroidTtsEngine.Voice>) -> AndroidTtsEngine.Voice? = defaultVoiceSelector,
        ): AndroidTtsNavigatorFactory? {
            val engineProvider = AndroidTtsEngineProvider(
                context = application,
                defaults = defaults,
                voiceSelector = voiceSelector
            )

            return createNavigatorFactory(
                application,
                publication,
                engineProvider,
                tokenizerFactory,
                metadataProvider
            )
        }

        public operator fun <
            S : TtsEngine.Settings,
            P : TtsEngine.Preferences<P>,
            E : PreferencesEditor<P>,
            F : TtsEngine.Error,
            V : TtsEngine.Voice,
            > invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, E, F, V>,
            tokenizerFactory: (language: Language?) -> TextTokenizer = defaultTokenizerFactory,
            metadataProvider: MediaMetadataProvider = defaultMediaMetadataProvider,
        ): TtsNavigatorFactory<S, P, E, F, V>? =
            createNavigatorFactory(
                application,
                publication,
                ttsEngineProvider,
                tokenizerFactory,
                metadataProvider
            )

        private fun <
            S : TtsEngine.Settings,
            P : TtsEngine.Preferences<P>,
            E : PreferencesEditor<P>,
            F : TtsEngine.Error,
            V : TtsEngine.Voice,
            > createNavigatorFactory(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, E, F, V>,
            tokenizerFactory: (language: Language?) -> TextTokenizer,
            metadataProvider: MediaMetadataProvider,
        ): TtsNavigatorFactory<S, P, E, F, V>? {
            publication.content() ?: return null

            return TtsNavigatorFactory(
                application,
                publication,
                ttsEngineProvider,
                tokenizerFactory,
                metadataProvider
            )
        }

        /**
         * The default content tokenizer will split the [Content.Element] items into individual sentences.
         */
        private val defaultTokenizerFactory: (Language?) -> TextTokenizer = { language ->
            DefaultTextContentTokenizer(TextUnit.Sentence, language)
        }

        private val defaultMediaMetadataProvider: MediaMetadataProvider =
            DefaultMediaMetadataProvider()

        private val defaultVoiceSelector: (Language?, Set<AndroidTtsEngine.Voice>) -> AndroidTtsEngine.Voice? =
            { _, _ -> null }
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?,
    ) : org.readium.r2.shared.util.Error {

        public class UnsupportedPublication(
            cause: org.readium.r2.shared.util.Error? = null,
        ) : Error("Publication is not supported.", cause)

        public class EngineInitialization(
            cause: org.readium.r2.shared.util.Error? = null,
        ) : Error("Failed to initialize TTS engine.", cause)
    }

    public suspend fun createNavigator(
        listener: TtsNavigator.Listener,
        initialLocator: Locator? = null,
        initialPreferences: P? = null,
    ): Try<TtsNavigator<S, P, F, V>, Error> {
        if (publication.findService(ContentService::class) == null) {
            return Try.failure(
                Error.UnsupportedPublication(
                    DebugError("No content service found in publication.")
                )
            )
        }

        @Suppress("NAME_SHADOWING")
        val initialLocator =
            initialLocator?.let { publication.normalizeLocator(it) }

        val actualInitialPreferences =
            initialPreferences
                ?: ttsEngineProvider.createEmptyPreferences()

        val contentIterator =
            TtsUtteranceIterator(publication, tokenizerFactory, initialLocator)
        if (!contentIterator.hasNext()) {
            return Try.failure(
                Error.UnsupportedPublication(
                    DebugError("Content iterator is empty.")
                )
            )
        }

        val ttsEngine =
            ttsEngineProvider.createEngine(publication, actualInitialPreferences)
                .getOrElse {
                    return Try.failure(
                        Error.EngineInitialization()
                    )
                }

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
                ?: return Try.failure(
                    Error.UnsupportedPublication(DebugError("Empty content."))
                )

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

        val ttsNavigator =
            TtsNavigator(coroutineScope, publication, ttsPlayer, sessionAdapter)

        return Try.success(ttsNavigator)
    }

    public fun createPreferencesEditor(preferences: P): E =
        ttsEngineProvider.createPreferencesEditor(publication, preferences)
}
