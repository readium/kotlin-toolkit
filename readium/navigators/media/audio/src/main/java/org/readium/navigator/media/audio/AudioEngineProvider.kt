/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.audio

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try

/**
 * To be implemented by adapters for third-party audio engines which can be used with [AudioNavigator].
 */
@ExperimentalReadiumApi
public interface AudioEngineProvider<
    S : Configurable.Settings,
    P : Configurable.Preferences<P>,
    E : PreferencesEditor<P>,
    > {

    public suspend fun createEngine(
        publication: Publication,
        initialLocator: Locator,
        initialPreferences: P,
    ): Try<AudioEngine<S, P>, Error>

    /**
     * Creates a preferences editor for [publication] and [initialPreferences].
     */
    public fun createPreferenceEditor(publication: Publication, initialPreferences: P): E

    /**
     * Creates an empty set of preferences of this TTS engine provider.
     */
    public fun createEmptyPreferences(): P
}
