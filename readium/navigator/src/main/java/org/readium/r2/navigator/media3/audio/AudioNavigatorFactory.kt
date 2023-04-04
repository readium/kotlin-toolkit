/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.audio

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
class AudioNavigatorFactory<S : Configurable.Settings, P : Configurable.Preferences<P>,
    E : PreferencesEditor<P>> private constructor(
    private val publication: Publication,
    private val audioEngineProvider: AudioEngineProvider<S, P, E>,
) {

    companion object {

        @Suppress("RedundantSuspendModifier")
        suspend operator fun <S : Configurable.Settings, P : Configurable.Preferences<P>,
            E : PreferencesEditor<P>> invoke(
            publication: Publication,
            audioEngineProvider: AudioEngineProvider<S, P, E>,
        ): AudioNavigatorFactory<S, P, E>? {
            if (!publication.conformsTo(Publication.Profile.AUDIOBOOK)) {
                return null
            }

            return AudioNavigatorFactory(
                publication, audioEngineProvider
            )
        }
    }

    suspend fun createNavigator(
        initialPreferences: P? = null,
        initialLocator: Locator? = null
    ): AudioBookNavigator<S, P>? {
        return AudioBookNavigator(
            publication = publication,
            audioEngineProvider = audioEngineProvider,
            initialPreferences = initialPreferences,
            initialLocator = initialLocator
        )
    }

    fun createAudioPreferencesEditor(
        currentPreferences: P,
    ): E = audioEngineProvider.createPreferenceEditor(publication, currentPreferences)
}
