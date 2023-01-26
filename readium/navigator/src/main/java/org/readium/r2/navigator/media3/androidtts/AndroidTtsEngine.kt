/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.androidtts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_ADD
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice as AndroidVoice
import android.speech.tts.Voice.*
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.media3.tts.TtsEngine
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
    private val defaultVoiceProvider: DefaultVoiceProvider?,
    initialPreferences: AndroidTtsPreferences
) : TtsEngine<AndroidTtsSettings, AndroidTtsPreferences,
        AndroidTtsEngine.Exception, AndroidTtsEngine.Voice> {

    companion object {

        suspend operator fun invoke(
            context: Context,
            metadata: Metadata,
            defaultVoiceProvider: DefaultVoiceProvider?,
            initialPreferences: AndroidTtsPreferences
        ): AndroidTtsEngine? {

            val init = CompletableDeferred<Boolean>()

            val listener = TextToSpeech.OnInitListener { status ->
                init.complete(status == TextToSpeech.SUCCESS)
            }
            val engine = TextToSpeech(context, listener)

            return if (init.await())
                AndroidTtsEngine(engine, metadata, defaultVoiceProvider, initialPreferences)
            else
                null
        }
    }

    fun interface DefaultVoiceProvider {

        fun chooseVoice(language: Language?, availableVoices: Set<Voice>): Voice?
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

    class Exception(code: Int) :
        kotlin.Exception("Android TTS engine error: $code"), TtsEngine.Error {

        val error: EngineError =
            EngineError.getOrDefault(code)
    }

    /**
     * Represents a voice provided by the TTS engine which can speak an utterance.
     *
     * @param name Unique and stable identifier for this voice
     * @param language Language (and region) this voice belongs to.
     * @param quality Voice quality.
     * @param requiresNetwork Indicates whether using this voice requires an Internet connection.
     */
    data class Voice(
        val name: String,
        override val language: Language,
        val quality: Quality = Quality.Normal,
        val requiresNetwork: Boolean = false,
    ) : TtsEngine.Voice {

        enum class Quality {
            Lowest, Low, Normal, High, Highest
        }
    }

    init {
        engine.setOnUtteranceProgressListener(Listener())
    }

    private var listener: TtsEngine.Listener<Exception>? =
        null

    private val settingsResolver: AndroidTtsSettingsResolver =
        AndroidTtsSettingsResolver(metadata)

    private val _settings: MutableStateFlow<AndroidTtsSettings> =
        MutableStateFlow(settingsResolver.settings(initialPreferences))

    override val voices: Set<Voice> get() =
        engine.voices
            .map { it.toTtsEngineVoice() }
            .toSet()

    override fun setListener(listener: TtsEngine.Listener<Exception>?) {
        this.listener = listener
    }

    override fun speak(
        requestId: String,
        text: String,
        language: Language?
    ) {
        engine.setupVoice(settings.value, language, voices)
        engine.speak(text, QUEUE_ADD, null, requestId)
    }

    override fun stop() {
        engine.stop()
    }

    override fun close() {
        engine.shutdown()
    }

    override val settings: StateFlow<AndroidTtsSettings> =
        _settings.asStateFlow()

    override fun submitPreferences(preferences: AndroidTtsPreferences) {
        val newSettings = settingsResolver.settings(preferences)
        engine.setupPitchAndSpeed(newSettings)
        _settings.value = newSettings
    }

    private fun TextToSpeech.setupPitchAndSpeed(settings: AndroidTtsSettings) {
        setSpeechRate(settings.speed.toFloat())
        setPitch(settings.pitch.toFloat())
    }

    private fun TextToSpeech.setupVoice(
        settings: AndroidTtsSettings,
        utteranceLanguage: Language?,
        voices: Set<Voice>
    ) {
        val language = utteranceLanguage
            ?: settings.language

        val preferredVoice = language
            ?.let { settings.voices[it] }
            ?.let { voiceForName(it) }

        val voice = preferredVoice
            ?: defaultVoice(language, voices)

        voice
            ?.let { engine.voice = it }
            ?: run { engine.language = language?.locale ?: Locale.getDefault() }
    }

    private fun defaultVoice(language: Language?, voices: Set<Voice>): AndroidVoice? =
        defaultVoiceProvider
            ?.chooseVoice(language, voices)
            ?.let { voiceForName(it.name) }

    private fun voiceForName(name: String) =
        engine.voices
            .firstOrNull { it.name == name }

    private fun AndroidVoice.toTtsEngineVoice() =
        Voice(
            name = name,
            language = Language(locale),
            quality = when (quality) {
                QUALITY_VERY_HIGH -> Voice.Quality.Highest
                QUALITY_HIGH -> Voice.Quality.High
                QUALITY_NORMAL -> Voice.Quality.Normal
                QUALITY_LOW -> Voice.Quality.Low
                QUALITY_VERY_LOW -> Voice.Quality.Lowest
                else -> throw IllegalStateException("Unexpected voice quality.")
            },
            requiresNetwork = isNetworkConnectionRequired
        )

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
            listener?.onError(
                utteranceId,
                Exception(errorCode)
            )
        }

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            listener?.onRange(utteranceId, start until end)
        }
    }
}
