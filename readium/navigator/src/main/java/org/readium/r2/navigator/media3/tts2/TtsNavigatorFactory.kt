/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import android.app.Application
import org.readium.r2.navigator.media3.api.DefaultMediaMetadataFactory
import org.readium.r2.navigator.media3.api.MediaMetadataFactory
import org.readium.r2.navigator.media3.api.MediaMetadataProvider
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.*
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.tokenizer.TextUnit

@ExperimentalReadiumApi
class TtsNavigatorFactory<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>, E : PreferencesEditor<P>,
    F : TtsEngine.Error, V : TtsEngine.Voice>(
    private val application: Application,
    private val publication: Publication,
    private val ttsEngineProvider: TtsEngineProvider<S, P, E, F, V>,
    private val tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer,
    private val metadataProvider: MediaMetadataProvider
) {
    companion object {

        suspend operator fun <S : TtsEngine.Settings, P : TtsEngine.Preferences<P>, E : PreferencesEditor<P>,
            F : TtsEngine.Error, V : TtsEngine.Voice> invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, E, F, V>,
            tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer = defaultTokenizerFactory,
            metadataProvider: MediaMetadataProvider = defaultMediaMetadataProvider
        ): TtsNavigatorFactory<S, P, E, F, V>? {

            publication.content()
                ?.iterator()
                ?.takeIf { it.hasNext() }
                ?: return null

            return TtsNavigatorFactory(application, publication, ttsEngineProvider, tokenizerFactory, metadataProvider)
        }

        /**
         * The default content tokenizer will split the [Content.Element] items into individual sentences.
         */
        val defaultTokenizerFactory: (Language?) -> ContentTokenizer = { language ->
            TextContentTokenizer(
                unit = TextUnit.Sentence,
                language = language,
                overrideContentLanguage = false
            )
        }

        val defaultMediaMetadataProvider: MediaMetadataProvider =
            object : MediaMetadataProvider {
                override fun createMetadataFactory(publication: Publication): MediaMetadataFactory {
                    return DefaultMediaMetadataFactory(publication)
                }
            }
    }

    suspend fun createNavigator(
        listener: TtsNavigatorListener,
        initialPreferences: P? = null,
        initialLocator: Locator? = null
    ): TtsNavigator<S, P, F, V> {
        return TtsNavigator(
            application,
            publication,
            ttsEngineProvider,
            tokenizerFactory,
            metadataProvider,
            listener,
            initialPreferences,
            initialLocator
        )!!
    }

    fun createTtsPreferencesEditor(
        currentPreferences: P,
    ): E = ttsEngineProvider.createPreferencesEditor(publication, currentPreferences)
}
