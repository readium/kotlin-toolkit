/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import android.app.Application
import org.readium.r2.navigator.media3.api.DefaultMetadataProvider
import org.readium.r2.navigator.media3.api.MetadataProvider
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.*
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.tokenizer.TextUnit

@ExperimentalReadiumApi
class TtsNavigatorFactory<S : TtsSettings, P : TtsPreferences<P>, E : PreferencesEditor<P>>(
    private val application: Application,
    private val publication: Publication,
    private val ttsEngineProvider: TtsEngineProvider<S, P, E>,
    private val tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer,
    private val metadataProvider: MetadataProvider
) {
    companion object {

        suspend operator fun <S : TtsSettings, P : TtsPreferences<P>, E : PreferencesEditor<P>> invoke(
            application: Application,
            publication: Publication,
            ttsEngineProvider: TtsEngineProvider<S, P, E>,
            tokenizerFactory: (defaultLanguage: Language?) -> ContentTokenizer = defaultTokenizerFactory,
            metadataProvider: MetadataProvider = defaultMetadataProvider
        ): TtsNavigatorFactory<S, P, E>? {

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

        val defaultMetadataProvider: MetadataProvider = DefaultMetadataProvider()
    }

    suspend fun createNavigator(
        listener: TtsNavigatorListener,
        initialPreferences: P? = null,
        initialLocator: Locator? = null
    ): TtsNavigator<S, P> {
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
