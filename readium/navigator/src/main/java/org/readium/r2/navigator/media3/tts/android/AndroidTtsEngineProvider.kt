/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts.android

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackException.*
import androidx.media3.common.PlaybackParameters
import org.readium.r2.navigator.media3.tts.TtsEngineProvider
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AndroidTtsEngineProvider(
    private val context: Context,
    private val defaults: AndroidTtsDefaults = AndroidTtsDefaults(),
    private val listener: AndroidTtsEngine.Listener? = null,
    private val voiceSelector: AndroidTtsEngine.VoiceSelector = AndroidTtsEngine.VoiceSelector { _, _ -> null }
) : TtsEngineProvider<AndroidTtsSettings, AndroidTtsPreferences, AndroidTtsPreferencesEditor,
        AndroidTtsEngine.Error, AndroidTtsEngine.Voice> {

    override suspend fun createEngine(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences
    ): AndroidTtsEngine? {
        val settingsResolver =
            AndroidTtsSettingsResolver(publication.metadata, defaults)

        return AndroidTtsEngine(
            context,
            settingsResolver,
            voiceSelector,
            listener,
            initialPreferences
        )
    }

    fun computeSettings(
        metadata: Metadata,
        preferences: AndroidTtsPreferences
    ): AndroidTtsSettings =
        AndroidTtsSettingsResolver(metadata, defaults).settings(preferences)

    override fun createPreferencesEditor(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences
    ): AndroidTtsPreferencesEditor =
        AndroidTtsPreferencesEditor(initialPreferences, publication.metadata, defaults)

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

    override fun mapEngineError(error: AndroidTtsEngine.Error): PlaybackException {
        val errorCode = when (error.kind) {
            AndroidTtsEngine.Error.Kind.Unknown ->
                ERROR_CODE_UNSPECIFIED
            AndroidTtsEngine.Error.Kind.InvalidRequest ->
                ERROR_CODE_IO_BAD_HTTP_STATUS
            AndroidTtsEngine.Error.Kind.Network ->
                ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            AndroidTtsEngine.Error.Kind.NetworkTimeout ->
                ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            AndroidTtsEngine.Error.Kind.NotInstalledYet ->
                ERROR_CODE_UNSPECIFIED
            AndroidTtsEngine.Error.Kind.Output ->
                ERROR_CODE_UNSPECIFIED
            AndroidTtsEngine.Error.Kind.Service ->
                ERROR_CODE_UNSPECIFIED
            AndroidTtsEngine.Error.Kind.Synthesis ->
                ERROR_CODE_UNSPECIFIED
        }

        val message = "Android TTS engine error: ${error.kind.code}"

        return PlaybackException(message, null, errorCode)
    }
}
