/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts

import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try

/**
 * To be implemented by adapters for third-party TTS engines which can be used with [TtsNavigator].
 */
@ExperimentalReadiumApi
public interface TtsEngineProvider<
    S : TtsEngine.Settings,
    P : TtsEngine.Preferences<P>,
    E : PreferencesEditor<P>,
    F : TtsEngine.Error,
    V : TtsEngine.Voice,
    > {

    /**
     * Creates a [TtsEngine] for [publication] and [initialPreferences].
     */
    public suspend fun createEngine(publication: Publication, initialPreferences: P): Try<TtsEngine<S, P, F, V>, Error>

    /**
     * Creates a preferences editor for [publication] and [initialPreferences].
     */
    public fun createPreferencesEditor(publication: Publication, initialPreferences: P): E

    /**
     * Creates an empty set of preferences of this TTS engine provider.
     */
    public fun createEmptyPreferences(): P

    /**
     * Computes Media3 [PlaybackParameters] from the given [settings].
     */
    public fun getPlaybackParameters(settings: S): PlaybackParameters

    /**
     * Updates [previousPreferences] to honor the given Media3 [playbackParameters].
     */
    public fun updatePlaybackParameters(
        previousPreferences: P,
        playbackParameters: PlaybackParameters,
    ): P

    /**
     * Maps an engine-specific error to Media3 [PlaybackException].
     */
    public fun mapEngineError(error: F): PlaybackException
}
