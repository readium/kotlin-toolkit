/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.*
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice as AndroidVoice
import android.speech.tts.Voice.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.media3.tts.TtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Default [TtsEngine] implementation using Android's native text to speech engine.
 */
@ExperimentalReadiumApi
class AndroidTtsEngine private constructor(
    private val engine: TextToSpeech,
    private val settingsResolver: SettingsResolver,
    private val voiceSelector: VoiceSelector,
    private val listener: Listener?,
    initialPreferences: AndroidTtsPreferences
) : TtsEngine<AndroidTtsSettings, AndroidTtsPreferences,
        AndroidTtsEngine.Error, AndroidTtsEngine.Voice> {

    companion object {

        suspend operator fun invoke(
            context: Context,
            settingsResolver: SettingsResolver,
            voiceSelector: VoiceSelector,
            listener: Listener?,
            initialPreferences: AndroidTtsPreferences
        ): AndroidTtsEngine? {

            val init = CompletableDeferred<Boolean>()

            val initListener = OnInitListener { status ->
                init.complete(status == SUCCESS)
            }
            val engine = TextToSpeech(context, initListener)

            return if (init.await())
                AndroidTtsEngine(engine, settingsResolver, voiceSelector, listener, initialPreferences)
            else
                null
        }

        /**
         * Starts the activity to install additional voice data.
         */
        fun requestInstallVoice(context: Context) {
            val intent = Intent()
                .setAction(Engine.ACTION_INSTALL_TTS_DATA)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val availableActivities =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(0)
                    )
                } else {
                    @Suppress("Deprecation")
                    context.packageManager.queryIntentActivities(intent, 0)
                }

            if (availableActivities.isNotEmpty()) {
                context.startActivity(intent)
            }
        }
    }

    fun interface SettingsResolver {

        /**
         * Computes a set of engine settings from the engine preferences.
         */
        fun settings(preferences: AndroidTtsPreferences): AndroidTtsSettings
    }

    fun interface VoiceSelector {

        /**
         * Selects a voice for the given [language].
         */
        fun voice(language: Language?, availableVoices: Set<Voice>): Voice?
    }

    class Error(code: Int) : TtsEngine.Error {

        val kind: Kind =
            Kind.getOrDefault(code)

        /**
         * Android's TTS error code.
         * See https://developer.android.com/reference/android/speech/tts/TextToSpeech#ERROR
         */
        enum class Kind(val code: Int) {
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

                fun getOrDefault(key: Int): Kind =
                    values()
                        .firstOrNull { it.code == key }
                        ?: Unknown
            }
        }
    }

    /**
     * Represents a voice provided by the TTS engine which can speak an utterance.
     *
     * @param id Unique and stable identifier for this voice
     * @param language Language (and region) this voice belongs to.
     * @param quality Voice quality.
     * @param requiresNetwork Indicates whether using this voice requires an Internet connection.
     */
    data class Voice(
        val id: Id,
        override val language: Language,
        val quality: Quality = Quality.Normal,
        val requiresNetwork: Boolean = false,
    ) : TtsEngine.Voice {

        @kotlinx.serialization.Serializable
        @JvmInline
        value class Id(val value: String)

        enum class Quality {
            Lowest, Low, Normal, High, Highest
        }
    }

    interface Listener {

        fun onMissingData(language: Language)

        fun onLanguageNotSupported(language: Language)
    }

    private val _settings: MutableStateFlow<AndroidTtsSettings> =
        MutableStateFlow(settingsResolver.settings(initialPreferences))

    private var utteranceListener: TtsEngine.Listener<Error>? =
        null

    override val voices: Set<Voice> get() =
        engine.voices
            ?.map { it.toTtsEngineVoice() }
            ?.toSet()
            .orEmpty()

    override fun setListener(
        listener: TtsEngine.Listener<Error>?
    ) {
        if (listener == null) {
            engine.setOnUtteranceProgressListener(null)
            this@AndroidTtsEngine.utteranceListener = null
        } else {
            this@AndroidTtsEngine.utteranceListener = listener
            engine.setOnUtteranceProgressListener(UtteranceListener(listener))
        }
    }

    override fun speak(
        requestId: TtsEngine.RequestId,
        text: String,
        language: Language?
    ) {
        engine.setupVoice(settings.value, language, voices)
        val queued = engine.speak(text, QUEUE_ADD, null, requestId.id)
        if (queued == ERROR) {
            utteranceListener?.onError(requestId, Error(Error.Kind.Unknown.code))
        }
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
            .takeUnless { settings.overrideContentLanguage }
            ?: settings.language

        when (engine.isLanguageAvailable(language.locale)) {
            LANG_MISSING_DATA -> listener?.onMissingData(language)
            LANG_NOT_SUPPORTED -> listener?.onLanguageNotSupported(language)
        }

        val preferredVoiceWithRegion =
            settings.voices[language]
                ?.let { voiceForName(it.value) }

        val preferredVoiceWithoutRegion =
            settings.voices[language.removeRegion()]
                ?.let { voiceForName(it.value) }

        val voice = preferredVoiceWithRegion
            ?: preferredVoiceWithoutRegion
            ?: defaultVoice(language, voices)

        voice
            ?.let { engine.voice = it }
            ?: run { engine.language = language.locale }
    }

    private fun defaultVoice(language: Language?, voices: Set<Voice>): AndroidVoice? =
        voiceSelector
            .voice(language, voices)
            ?.let { voiceForName(it.id.value) }

    private fun voiceForName(name: String) =
        engine.voices
            .firstOrNull { it.name == name }

    private fun AndroidVoice.toTtsEngineVoice() =
        Voice(
            id = Voice.Id(name),
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

    class UtteranceListener(
        private val listener: TtsEngine.Listener<Error>?
    ) : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            listener?.onStart(TtsEngine.RequestId(utteranceId))
        }

        override fun onStop(utteranceId: String, interrupted: Boolean) {
            listener?.let {
                val requestId = TtsEngine.RequestId(utteranceId)
                if (interrupted) {
                    it.onInterrupted(requestId)
                } else {
                    it.onFlushed(requestId)
                }
            }
        }

        override fun onDone(utteranceId: String) {
            listener?.onDone(TtsEngine.RequestId(utteranceId))
        }

        @Deprecated("Deprecated in the interface", ReplaceWith("onError(utteranceId, -1)"))
        override fun onError(utteranceId: String) {
            onError(utteranceId, -1)
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            listener?.onError(
                TtsEngine.RequestId(utteranceId),
                Error(errorCode)
            )
        }

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            listener?.onRange(TtsEngine.RequestId(utteranceId), start until end)
        }
    }
}
