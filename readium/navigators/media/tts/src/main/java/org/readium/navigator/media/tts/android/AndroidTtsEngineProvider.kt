/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts.android

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackException.*
import androidx.media3.common.PlaybackParameters
import org.readium.navigator.media.tts.TtsEngineProvider
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try

@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
public class AndroidTtsEngineProvider(
    private val context: Context,
    private val defaults: AndroidTtsDefaults = AndroidTtsDefaults(),
    private val voiceSelector: AndroidTtsEngine.VoiceSelector = AndroidTtsEngine.VoiceSelector { _, _ -> null },
) : TtsEngineProvider<
    AndroidTtsSettings,
    AndroidTtsPreferences,
    AndroidTtsPreferencesEditor,
    AndroidTtsEngine.Error,
    AndroidTtsEngine.Voice
    > {

    override suspend fun createEngine(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences,
    ): Try<AndroidTtsEngine, Error> {
        val settingsResolver =
            AndroidTtsSettingsResolver(publication.metadata, defaults)

        val engine = AndroidTtsEngine(
            context,
            settingsResolver,
            voiceSelector,
            initialPreferences
        ) ?: return Try.failure(
            DebugError("Initialization of Android Tts service failed.")
        )

        return Try.success(engine)
    }

    override fun createPreferencesEditor(
        publication: Publication,
        initialPreferences: AndroidTtsPreferences,
    ): AndroidTtsPreferencesEditor =
        AndroidTtsPreferencesEditor(initialPreferences, publication.metadata, defaults)

    override fun createEmptyPreferences(): AndroidTtsPreferences =
        AndroidTtsPreferences()

    override fun getPlaybackParameters(
        settings: AndroidTtsSettings,
    ): PlaybackParameters {
        return PlaybackParameters(settings.speed.toFloat(), settings.pitch.toFloat())
    }

    override fun updatePlaybackParameters(
        previousPreferences: AndroidTtsPreferences,
        playbackParameters: PlaybackParameters,
    ): AndroidTtsPreferences {
        return previousPreferences.copy(
            speed = playbackParameters.speed.toDouble(),
            pitch = playbackParameters.pitch.toDouble()
        )
    }

    override fun mapEngineError(error: AndroidTtsEngine.Error): PlaybackException {
        val errorCode = when (error) {
            AndroidTtsEngine.Error.Unknown ->
                ERROR_CODE_UNSPECIFIED
            AndroidTtsEngine.Error.InvalidRequest ->
                ERROR_CODE_IO_BAD_HTTP_STATUS
            AndroidTtsEngine.Error.Network ->
                ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            AndroidTtsEngine.Error.NetworkTimeout ->
                ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            AndroidTtsEngine.Error.Output,
            AndroidTtsEngine.Error.Service,
            AndroidTtsEngine.Error.Synthesis,
            is AndroidTtsEngine.Error.LanguageMissingData,
            AndroidTtsEngine.Error.NotInstalledYet,
            ->
                ERROR_CODE_UNSPECIFIED
        }

        val message = "Android TTS engine error: ${error.javaClass.simpleName}"

        return PlaybackException(message, null, errorCode)
    }
}
