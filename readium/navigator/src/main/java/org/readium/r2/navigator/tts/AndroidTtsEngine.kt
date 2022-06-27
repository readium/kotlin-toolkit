/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.tts

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.tts.TtsEngine.Configuration
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.MapWithDefaultCompanion
import java.util.*

class AndroidTtsEngine(
    context: Context,
    config: Configuration = Configuration(),
    private val listener: TtsEngine.Listener
) : TtsEngine {

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

    class EngineException(code: Int) : Exception("Android TTS engine error: $code") {
        val error: EngineError =
            EngineError.getOrDefault(code)
    }

    private val scope = MainScope()

    private val engineListener = EngineListener()

    private val engine = TextToSpeech(context, engineListener).apply {
        setOnUtteranceProgressListener(engineListener)
        setConfig(config)
    }

    /**
     * Start the activity to install additional language data.
     * To be called if you receive a [TtsEngine.Exception.LanguageSupportIncomplete] error.
     *
     * Returns whether the request was successful.
     *
     * See https://android-developers.googleblog.com/2009/09/introduction-to-text-to-speech-in.html
     */
    fun requestInstallMissingVoice(context: Context, intentFlags: Int = Intent.FLAG_ACTIVITY_NEW_TASK): Boolean {
        val intent = Intent()
            .setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            .setFlags(intentFlags)

        if (context.packageManager.queryIntentActivities(intent, 0).isEmpty()) {
            return false
        }

        context.startActivity(intent)
        return true
    }

    private val _config = MutableStateFlow(config)
    override val config: StateFlow<Configuration> = _config.asStateFlow()

    override fun setConfig(config: Configuration): Configuration {
        engine.setConfig(config)
        _config.value = config
        return config
    }

    private var _availableLocales = MutableStateFlow(emptySet<Locale>())
    override val availableLocales: StateFlow<Set<Locale>> = _availableLocales.asStateFlow()

    private var _availableVoices = MutableStateFlow(emptySet<TtsEngine.Voice>())
    override val availableVoices: StateFlow<Set<TtsEngine.Voice>> = _availableVoices.asStateFlow()

    override fun voiceWithIdentifier(identifier: String): TtsEngine.Voice? =
        availableVoices.value.firstOrNull { it.identifier == identifier }

    private var speakJob: Job? = null

    override fun speak(utterance: TtsEngine.Utterance) {
        speakJob?.cancel()
        speakJob = scope.launch {
            init.await()

            val locale = utterance.localeOrDefault

            val localeResult = engine.setLanguage(locale)
            if (localeResult < TextToSpeech.LANG_AVAILABLE) {
                val error =
                    if (localeResult == TextToSpeech.LANG_MISSING_DATA)
                        TtsEngine.Exception.LanguageSupportIncomplete(locale)
                    else
                        TtsEngine.Exception.LanguageNotSupported(locale)

                listener.onUtteranceError(utterance, error)
                return@launch
            }

            val id = nextId()
            utterances[id] = utterance
            engine.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, null, id)
        }
    }

    override fun stop() {
        speakJob?.cancel()
        speakJob = null

        if (init.isCompleted) {
            tryOrLog {
                engine.stop()
            }
        }
    }

    override suspend fun close() {
        stop()
        engine.shutdown()
        scope.cancel()
    }

    private val utterances = mutableMapOf<String, TtsEngine.Utterance>()

    private var idCount: Int = 0

    private fun nextId(): String =
        idCount++.toString()

    private val TtsEngine.Utterance.localeOrDefault: Locale get() =
        language
            ?: config.value.defaultLocale
            ?: engine.voice.locale

    // Engine

    private val init = CompletableDeferred<Unit>()

    private fun TextToSpeech.setConfig(config: Configuration) {
        setSpeechRate(config.rate.toFloat())
    }

    private inner class EngineListener : TextToSpeech.OnInitListener, UtteranceProgressListener() {
        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        _availableLocales.value = engine.availableLanguages

                        _availableVoices.value = engine.voices
                            .map {
                                TtsEngine.Voice(
                                    identifier = it.name,
                                    name = it.name,
                                    locale = it.locale,
                                    quality = when (it.quality) {
                                        Voice.QUALITY_VERY_HIGH -> TtsEngine.Voice.Quality.Highest
                                        Voice.QUALITY_HIGH -> TtsEngine.Voice.Quality.High
                                        Voice.QUALITY_LOW -> TtsEngine.Voice.Quality.Low
                                        Voice.QUALITY_VERY_LOW -> TtsEngine.Voice.Quality.Lowest
                                        else -> TtsEngine.Voice.Quality.Normal
                                    },
                                    requiresNetwork = it.isNetworkConnectionRequired
                                )
                            }
                            .toSet()
                    }

                    init.complete(Unit)
                }
            } else {
                listener.onEngineError(TtsEngine.Exception.InitializationFailed())
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

        @Deprecated("Deprecated in the interface", ReplaceWith("onError(utteranceId, -1)"))
        override fun onError(utteranceId: String?) {
            onError(utteranceId, -1)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            val utterance = utterances.remove(utteranceId) ?: return
            val error = EngineException(errorCode)
            listener.onUtteranceError(utterance, when (error.error) {
                EngineError.Network, EngineError.NetworkTimeout -> TtsEngine.Exception.Network(error)
                EngineError.NotInstalledYet -> TtsEngine.Exception.LanguageSupportIncomplete(utterance.localeOrDefault)
                else -> TtsEngine.Exception.Other(error)
            })
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
