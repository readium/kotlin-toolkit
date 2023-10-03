/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigators.media.audio

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
public class AudioNavigatorFactory<S : Configurable.Settings, P : Configurable.Preferences<P>,
    E : PreferencesEditor<P>> private constructor(
    private val publication: Publication,
    private val audioEngineProvider: AudioEngineProvider<S, P, E>
) {

    public companion object {

        @Suppress("RedundantSuspendModifier")
        public suspend operator fun <S : Configurable.Settings, P : Configurable.Preferences<P>,
            E : PreferencesEditor<P>> invoke(
            publication: Publication,
            audioEngineProvider: AudioEngineProvider<S, P, E>
        ): AudioNavigatorFactory<S, P, E>? {
            if (!publication.conformsTo(Publication.Profile.AUDIOBOOK)) {
                return null
            }

            if (publication.readingOrder.any { it.duration == 0.0 }) {
                return null
            }

            return AudioNavigatorFactory(
                publication,
                audioEngineProvider
            )
        }
    }

    public suspend fun createNavigator(
        initialLocator: Locator? = null,
        initialPreferences: P? = null
    ): AudioNavigator<S, P>? {
        return AudioNavigator(
            publication = publication,
            audioEngineProvider = audioEngineProvider,
            initialLocator = initialLocator,
            initialPreferences = initialPreferences
        )
    }

    public fun createAudioPreferencesEditor(
        currentPreferences: P
    ): E = audioEngineProvider.createPreferenceEditor(publication, currentPreferences)
}
