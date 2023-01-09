/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_ADD
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.media3.tts2.TtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Language

/**
 * Default [TtsEngine] implementation using Android's native text to speech engine.
 */
@ExperimentalReadiumApi
class AndroidTtsEngine(
    private val engine: TextToSpeech,
    metadata: Metadata,
    initialPreferences: AndroidTtsPreferences
) : TtsEngine<AndroidTtsSettings, AndroidTtsPreferences> {

    companion object {

        suspend operator fun invoke(
            context: Context,
            metadata: Metadata,
            initialPreferences: AndroidTtsPreferences
        ): AndroidTtsEngine? {

            val init = CompletableDeferred<Boolean>()

            val listener = TextToSpeech.OnInitListener { status ->
                init.complete(status == TextToSpeech.SUCCESS)
            }
            val engine = TextToSpeech(context, listener)

            return if (init.await())
                AndroidTtsEngine(engine, metadata, initialPreferences)
            else
                null
        }
    }

    /**
     * Android's TTS error code.
     * See https://developer.android.com/reference/android/speech/tts/TextToSpeech#ERROR
     */
    enum class EngineError(val code: Int) {
        /** Denotes a generic operation failure. */
        Unknown(-1),
        /** Denotes a failure caused by an invalid request. */
        InvalidRequest(-8),
        /** Denotes a failure caused by a network connectivity problems. */
        Network(-6),
        /** Denotes a failure caused by network timeout. */
        NetworkTimeout(-7),
        /** Denotes a failure caused by an unfinished download of the voice data. */
        NotInstalledYet(-9),
        /** Denotes a failure related to the output (audio device or a file). */
        Output(-5),
        /** Denotes a failure of a TTS service. */
        Service(-4),
        /** Denotes a failure of a TTS engine to synthesize the given input. */
        Synthesis(-3);

        companion object {
            fun getOrDefault(key: Int): EngineError =
                values()
                    .firstOrNull { it.code == key }
                    ?: Unknown
        }
    }

    class EngineException(code: Int) : Exception("Android TTS engine error: $code") {
        val error: EngineError =
            EngineError.getOrDefault(code)
    }

    init {
        engine.setOnUtteranceProgressListener(Listener())
    }

    private var listener: TtsEngine.Listener? =
        null

    private val settingsResolver: AndroidTtsSettingsResolver =
        AndroidTtsSettingsResolver(metadata)

    private val _settings: MutableStateFlow<AndroidTtsSettings> =
        MutableStateFlow(settingsResolver.settings(initialPreferences))

    override fun close() {
        engine.shutdown()
    }

    override fun speak(
        requestId: String,
        text: String,
        language: Language?
    ) {
        engine.language = language?.locale ?: settings.value.language?.locale
        engine.speak(text, QUEUE_ADD, null, requestId)
    }

    /**
     * Start the activity to install additional language data.
     * To be called if you receive a [TtsEngine.Exception.LanguageSupportIncomplete] error.
     *
     * Returns whether the request was successful.
     *
     * See https://android-developers.googleblog.com/2009/09/introduction-to-text-to-speech-in.html
     */
    fun requestInstallMissingVoice(
        context: Context,
        intentFlags: Int = Intent.FLAG_ACTIVITY_NEW_TASK
    ): Boolean {
        val intent = Intent()
            .setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            .setFlags(intentFlags)

        if (context.packageManager.queryIntentActivities(intent, 0).isEmpty()) {
            return false
        }

        context.startActivity(intent)
        return true
    }

    // Engine

    override fun stop() {
        engine.stop()
    }

    override fun setListener(listener: TtsEngine.Listener?) {
        this.listener = listener
    }

    override val settings: StateFlow<AndroidTtsSettings> =
        _settings.asStateFlow()

    override fun submitPreferences(preferences: AndroidTtsPreferences) {
        val newSettings = settingsResolver.settings(preferences)
        engine.setup(newSettings)
        _settings.value = newSettings
    }

    private fun TextToSpeech.setup(settings: AndroidTtsSettings) {
        setSpeechRate(settings.speed.toFloat())
        setPitch(settings.pitch.toFloat())

        val localeResult = engine.setLanguage(settings.language?.locale)
        if (localeResult < TextToSpeech.LANG_AVAILABLE) {
            if (localeResult == TextToSpeech.LANG_MISSING_DATA)
                throw org.readium.r2.navigator.tts.TtsEngine.Exception.LanguageSupportIncomplete(settings.language!!)
            else
                throw org.readium.r2.navigator.tts.TtsEngine.Exception.LanguageNotSupported(settings.language!!)
        }
    }

    inner class Listener : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            listener?.onStart(utteranceId)
        }

        override fun onStop(utteranceId: String, interrupted: Boolean) {
            listener?.let {
                if (interrupted) {
                    it.onInterrupted(utteranceId)
                } else {
                    it.onFlushed(utteranceId)
                }
            }
        }

        override fun onDone(utteranceId: String) {
            listener?.onDone(utteranceId)
        }

        @Deprecated("Deprecated in the interface", ReplaceWith("onError(utteranceId, -1)"))
        override fun onError(utteranceId: String) {
            onError(utteranceId, -1)
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            // listener?.onError(utteranceId!!, EngineException(errorCode))
        }

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            listener?.onRange(utteranceId, start until end)
        }
    }
}
