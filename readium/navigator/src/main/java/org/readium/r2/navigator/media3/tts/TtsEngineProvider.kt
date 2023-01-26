/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts

import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

/**
 * To be implemented by adapters for third-party TTS engines which can be used with [TtsNavigator].
 */
@ExperimentalReadiumApi
interface TtsEngineProvider<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>, E : PreferencesEditor<P>,
    F : TtsEngine.Error, V : TtsEngine.Voice> {

    suspend fun createEngine(publication: Publication, initialPreferences: P): TtsEngine<S, P, F, V>?

    fun createPreferencesEditor(publication: Publication, initialPreferences: P): E

    fun createEmptyPreferences(): P

    fun getPlaybackParameters(settings: S): PlaybackParameters

    fun updatePlaybackParameters(previousPreferences: P, playbackParameters: PlaybackParameters): P

    fun mapEngineError(error: F): PlaybackException
}
