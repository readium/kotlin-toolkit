/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.player

import org.readium.r2.navigator.media3.api.MetadataProvider
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
class PlayerNavigatorFactory<S : Configurable.Settings, P : Configurable.Preferences<P>, E : PreferencesEditor<P>>(
    private val publication: Publication,
    private val mediaEngineProvider: MediaEngineProvider<S, P, E>,
    private val playerNavigator: PlayerNavigator<S, P>,
) {

    companion object {

        suspend operator fun <S : Configurable.Settings, P : Configurable.Preferences<P>, E : PreferencesEditor<P>> invoke(
            publication: Publication,
            mediaEngineProvider: MediaEngineProvider<S, P, E>,
            metadataProvider: MetadataProvider,
            initialPreferences: P,
            initialLocator: Locator
        ): PlayerNavigatorFactory<S, P, E> {

            val navigator = PlayerNavigator(
                publication,
                mediaEngineProvider,
                metadataProvider.createMetadataFactory(publication),
                initialPreferences,
                initialLocator,
            )

            return PlayerNavigatorFactory(
                publication,
                mediaEngineProvider,
                navigator
            )
        }
    }

    fun getMediaNavigator(): PlayerNavigator<S, P> =
        playerNavigator

    fun createPreferencesEditor(
        initialPreferences: P
    ): E =
        mediaEngineProvider.createPreferenceEditor(
            publication,
            initialPreferences
        )
}
