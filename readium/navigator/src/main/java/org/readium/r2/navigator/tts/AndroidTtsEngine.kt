/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.tts.TtsEngine.Configuration
import org.readium.r2.shared.util.MapWithDefaultCompanion
import java.util.*

class AndroidTtsEngine(
    context: Context,
    config: Configuration = Configuration(),
    private val listener: TtsEngine.Listener
) : TtsEngine {

    sealed class Exception private constructor(
        override val message: String,
        cause: Throwable? = null
    ) : kotlin.Exception(message, cause) {
        object InitializationFailed : Exception("The Android TTS engine failed to initialize")
        class LanguageNotSupported(val locale: Locale) : Exception("The language ${locale.toLanguageTag()} is not supported by the Android TTS engine")
        class LanguageMissingData(val locale: Locale) : Exception("The language ${locale.toLanguageTag()} requires additional files by the Android TTS engine")
        class UtteranceError(val utterance: TtsEngine.Utterance, val error: EngineError) : Exception("Failed to play the utterance: $error")
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

        companion object : MapWithDefaultCompanion<Int, EngineError>(values(), EngineError::code, Unknown)
    }

    private val _config = MutableStateFlow(config)
    override val config: StateFlow<Configuration> = _config.asStateFlow()

    override suspend fun setConfig(config: Configuration): Configuration {
        engine.setConfig(config)
        _config.value = config
        return config
    }

    override suspend fun speak(utterance: TtsEngine.Utterance) {
        init.await()

        val locale = utterance.language
            ?: config.value.defaultLocale
            ?: engine.voice.locale

        val localeResult = engine.setLanguage(locale)
        if (localeResult < TextToSpeech.LANG_AVAILABLE) {
            val error =
                if (localeResult == TextToSpeech.LANG_MISSING_DATA)
                    Exception.LanguageMissingData(locale)
                else
                    Exception.LanguageNotSupported(locale)

            listener.onError(error)
            throw error
        }

        val id = nextId()
        utterances[id] = utterance
        engine.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    override suspend fun stop() {
        init.await()
        engine.stop()
    }

    override suspend fun close() {
        stop()
        engine.shutdown()
    }

    private val utterances = mutableMapOf<String, TtsEngine.Utterance>()

    private var idCount: Int = 0

    private fun nextId(): String =
        idCount++.toString()

    // Engine

    private val init = CompletableDeferred<Unit>()
    private val engineListener = EngineListener()

    private val engine = TextToSpeech(context, engineListener).apply {
        setOnUtteranceProgressListener(engineListener)
        setConfig(config)
    }

    private fun TextToSpeech.setConfig(config: Configuration) {
        setSpeechRate(config.rate.toFloat())
    }

    private inner class EngineListener : TextToSpeech.OnInitListener, UtteranceProgressListener() {
        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                init.complete(Unit)
            } else {
                listener.onError(Exception.InitializationFailed)
            }
        }

        override fun onStart(utteranceId: String?) {}

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            utterances.remove(utteranceId)
        }

        override fun onDone(utteranceId: String?) {
            utterances.remove(utteranceId)
            listener.onStop()
        }

        @Deprecated("Deprecated in the interface")
        override fun onError(utteranceId: String?) {
            onError(utteranceId, -1)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            val utterance = utterances.remove(utteranceId) ?: return
            listener.onError(Exception.UtteranceError(utterance, EngineError.getOrDefault(errorCode)))
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            val utterance = utterances[utteranceId] ?: return

            listener.onSpeakRangeAt(
                locator = utterance.locator.copy(
                    text = utterance.locator.text.substring(start, end)
                ),
                utterance = utterance
            )
        }
    }
}

//    var voice: Voice
//        get() = tts.voice
//        set(value) { tts.voice = value }
//
//    val voices: Map<Locale, List<Voice>> get() =
//        tts.voices.groupBy(Voice::getLocale)
//
//    val defaultVoice: Voice
//        get() = tts.defaultVoice
//
//        val locale = span.language?.let { Locale.forLanguageTag(it.replace("_", "-")) }
//            ?: defaultLocale
