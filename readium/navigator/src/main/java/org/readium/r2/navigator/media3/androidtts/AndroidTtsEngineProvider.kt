/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackException.*
import androidx.media3.common.PlaybackParameters
import org.readium.r2.navigator.media3.tts2.TtsEngineProvider
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AndroidTtsEngineProvider(
    private val context: Context,
    private val defaultVoiceProvider: AndroidTtsEngine.DefaultVoiceProvider? = null
) : TtsEngineProvider<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsPreferencesEditor,
        AndroidTtsEngine.Exception, AndroidTtsEngine.Voice> {

    override suspend fun createEngine(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences
    ): AndroidTtsEngine? {
        return AndroidTtsEngine(
            context,
            publication.metadata,
            defaultVoiceProvider,
            initialPreferences
        )
    }

    fun computeSettings(
        metadata: Metadata,
        preferences: AndroidTtsPreferences
    ): AndroidTtsSettings =
        AndroidTtsSettingsResolver(metadata).settings(preferences)

    override fun createPreferencesEditor(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences
    ): AndroidTtsPreferencesEditor =
        AndroidTtsPreferencesEditor(initialPreferences, publication.metadata)

    override fun createEmptyPreferences(): AndroidTtsPreferences =
        AndroidTtsPreferences()

    override fun getPlaybackParameters(
        settings: AndroidTtsSettings
    ): PlaybackParameters {
        return PlaybackParameters(settings.speed.toFloat(), settings.pitch.toFloat())
    }

    override fun updatePlaybackParameters(
        previousPreferences: AndroidTtsPreferences,
        playbackParameters: PlaybackParameters
    ): AndroidTtsPreferences {
        return previousPreferences.copy(
            speed = playbackParameters.speed.toDouble(),
            pitch = playbackParameters.pitch.toDouble()
        )
    }

    override fun mapEngineError(error: AndroidTtsEngine.Exception): PlaybackException =
        when (error.error) {
            AndroidTtsEngine.EngineError.Unknown ->
                PlaybackException(error.message, error.cause, ERROR_CODE_UNSPECIFIED)
            AndroidTtsEngine.EngineError.InvalidRequest ->
                PlaybackException(error.message, error.cause, ERROR_CODE_IO_BAD_HTTP_STATUS)
            AndroidTtsEngine.EngineError.Network ->
                PlaybackException(error.message, error.cause, ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
            AndroidTtsEngine.EngineError.NetworkTimeout ->
                PlaybackException(error.message, error.cause, ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
            AndroidTtsEngine.EngineError.NotInstalledYet ->
                PlaybackException(error.message, error.cause, ERROR_CODE_UNSPECIFIED)
            AndroidTtsEngine.EngineError.Output ->
                PlaybackException(error.message, error.cause, ERROR_CODE_UNSPECIFIED)
            AndroidTtsEngine.EngineError.Service ->
                PlaybackException(error.message, error.cause, ERROR_CODE_UNSPECIFIED)
            AndroidTtsEngine.EngineError.Synthesis ->
                PlaybackException(error.message, error.cause, ERROR_CODE_UNSPECIFIED)
        }
}
