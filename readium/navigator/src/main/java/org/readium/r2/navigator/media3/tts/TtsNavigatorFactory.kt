/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts

import android.app.Application
import org.readium.r2.navigator.media3.api.DefaultMediaMetadataProvider
import org.readium.r2.navigator.media3.api.MediaMetadataProvider
import org.readium.r2.navigator.media3.tts.android.AndroidTtsDefaults
import org.readium.r2.navigator.media3.tts.android.AndroidTtsEngine
import org.readium.r2.navigator.media3.tts.android.AndroidTtsEngineProvider
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.content
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.tokenizer.DefaultTextContentTokenizer
import org.readium.r2.shared.util.tokenizer.TextTokenizer
import org.readium.r2.shared.util.tokenizer.TextUnit

@ExperimentalReadiumApi
class TtsNavigatorFactory<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>, E : PreferencesEditor<P>,
    F : TtsEngine.Error, V : TtsEngine.Voice> private constructor(
    private val application: Application,
    private val publication: Publication,
    private val ttsEngineProvider: TtsEngineProvider<S, P, E, F, V>,
    private val tokenizerFactory: (language: Language?) -> TextTokenizer,
    private val metadataProvider: MediaMetadataProvider
) {
    companion object {

        suspend operator fun invoke(
            application: Application,
            publication: Publication,
            tokenizerFactory: (language: Language?) -> TextTokenizer = defaultTokenizerFactory,
            metadataProvider: MediaMetadataProvider = defaultMediaMetadataProvider,
            defaults: AndroidTtsDefaults = AndroidTtsDefaults(),
            voiceSelector: (Language?, Set<AndroidTtsEngine.Voice>) -> AndroidTtsEngine.Voice? = defaultVoiceSelector,
            listener: AndroidTtsEngine.Listener? = null
        ): AndroidTtsNavigatorFactory? {

            val engineProvider = AndroidTtsEngineProvider(
                context = application,
                defaults = defaults,
                voiceSelector = voiceSelector,
                listener = listener
            )

            return createNavigatorFactory(
                application,
                publication,
                engineProvider,
                tokenizerFactory,
                metadataProvider
            )
        }

        suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>, E : PreferencesEditor<P>,
            F : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, E, F, V>,
            tokenizerFactory: (language: Language?) -> TextTokenizer = defaultTokenizerFactory,
            metadataProvider: MediaMetadataProvider = defaultMediaMetadataProvider
        ): TtsNavigatorFactory<S, P, E, F, V>? {

            return createNavigatorFactory(
                application,
                publication,
                ttsEngineProvider,
                tokenizerFactory,
                metadataProvider
            )
        }

        private suspend fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>, E : PreferencesEditor<P>,
            F : TtsEngine.Error, V : TtsEngine.Voice> createNavigatorFactory(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, E, F, V>,
            tokenizerFactory: (language: Language?) -> TextTokenizer,
            metadataProvider: MediaMetadataProvider
        ): TtsNavigatorFactory<S, P, E, F, V>? {

            publication.content()
                ?.iterator()
                ?.takeIf { it.hasNext() }
                ?: return null

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

    suspend fun createNavigator(
        listener: TtsNavigator.Listener,
        initialPreferences: P? = null,
        initialLocator: Locator? = null
    ): TtsNavigator<S, P, F, V>? {
        return TtsNavigator(
            application,
            publication,
            ttsEngineProvider,
            tokenizerFactory,
            metadataProvider,
            listener,
            initialPreferences,
            initialLocator
        )
    }

    fun createTtsPreferencesEditor(
        currentPreferences: P,
    ): E = ttsEngineProvider.createPreferencesEditor(publication, currentPreferences)
}
