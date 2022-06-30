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
import kotlinx.coroutines.flow.update
import org.readium.r2.navigator.tts.TtsEngine.Configuration
import org.readium.r2.navigator.tts.TtsEngine.ConfigurationConstraints
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.MapWithDefaultCompanion

@OptIn(InternalReadiumApi::class)
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    private var _configConstraints = MutableStateFlow(ConfigurationConstraints(
        rateRange = 0.1..3.0
    ))
    override val configConstraints: StateFlow<ConfigurationConstraints> = _configConstraints.asStateFlow()

    override fun setConfig(config: Configuration): Configuration {
        engine.setConfig(config)
        _config.value = config
        return config
    }

    private suspend fun updateConfigConstraints() = withContext(Dispatchers.Default) {
        _configConstraints.update { constraints ->
            constraints.copy(
                availableVoices = engine.voices.map { it.toVoice() }
            )
        }
    }

    private var speakJob: Job? = null

    override fun speak(utterance: TtsEngine.Utterance) {
        speakJob?.cancel()
        speakJob = scope.launch {
            init.await()

            if (!setupVoiceForUtterance(utterance)) {
                return@launch
            }

            val id = nextId()
            utterances[id] = utterance
            engine.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, null, id)
        }
    }

    private fun setupVoiceForUtterance(utterance: TtsEngine.Utterance): Boolean {
        // Setup the user selected voice.
        val voice = config.value.voice
        val language = utterance.languageOrDefault
        if (voice != null && voice.language.removeRegion() == language.removeRegion()) {
            engine.voices
                .firstOrNull { it.name == voice.identifier }
                ?.let {
                    engine.voice = it
                    return true
                }
        }

        // Or fallback on the language.
        val localeResult = engine.setLanguage(language.locale)
        if (localeResult < TextToSpeech.LANG_AVAILABLE) {
            val error =
                if (localeResult == TextToSpeech.LANG_MISSING_DATA)
                    TtsEngine.Exception.LanguageSupportIncomplete(language)
                else
                    TtsEngine.Exception.LanguageNotSupported(language)

            listener.onUtteranceError(utterance, error)
            return false
        }

        return true
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

    private val TtsEngine.Utterance.languageOrDefault: Language get() =
        language
            ?: config.value.defaultLanguage
            ?: Language(engine.voice.locale)

    // Engine

    private val init = CompletableDeferred<Unit>()

    private fun TextToSpeech.setConfig(config: Configuration) {
        setSpeechRate(config.rate.toFloat())
    }

    private inner class EngineListener : TextToSpeech.OnInitListener, UtteranceProgressListener() {
        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                scope.launch {
                    updateConfigConstraints()
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
                EngineError.NotInstalledYet -> TtsEngine.Exception.LanguageSupportIncomplete(utterance.languageOrDefault)
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

private fun Voice.toVoice(): TtsEngine.Voice =
    TtsEngine.Voice(
        identifier = name,
        name = null,
        language = Language(locale),
        quality = when (quality) {
            Voice.QUALITY_VERY_HIGH -> TtsEngine.Voice.Quality.Highest
            Voice.QUALITY_HIGH -> TtsEngine.Voice.Quality.High
            Voice.QUALITY_LOW -> TtsEngine.Voice.Quality.Low
            Voice.QUALITY_VERY_LOW -> TtsEngine.Voice.Quality.Lowest
            else -> TtsEngine.Voice.Quality.Normal
        },
        requiresNetwork = isNetworkConnectionRequired
    )
