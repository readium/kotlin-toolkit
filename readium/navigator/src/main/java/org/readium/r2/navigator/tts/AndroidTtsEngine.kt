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
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.navigator.tts.TtsEngine.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.MapWithDefaultCompanion
import kotlin.Exception
import android.speech.tts.Voice as AndroidVoice

/**
 * Default [TtsEngine] implementation using Android's native text to speech engine.
 */
@ExperimentalReadiumApi
class AndroidTtsEngine(
    context: Context,
    private val listener: Listener
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

    override val rateRange: ClosedRange<Double> = 0.1..3.0
    override var availableVoices: List<Voice> = emptyList()
        private set

    private var speakJob: Job? = null
    private val mutex = Mutex()

    override fun speak(utterance: Utterance) {
        cancel()
        speakJob = scope.launch {
            init.await()

            mutex.withLock {
                speakSync(utterance)
            }
        }
    }

    private fun speakSync(utterance: Utterance) {
        try {
            engine.setupFor(utterance)
            engine.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, null, utterance.id)

        } catch (e: kotlin.Exception) {
            listener.onUtteranceError(utterance.id, TtsEngine.Exception.wrap(e))
        }
    }

    private fun TextToSpeech.setupFor(utterance: Utterance) {
        setSpeechRate(utterance.rate.toFloat())

        utterance.voiceOrLanguage
            .onLeft { voice ->
                // Setup the user selected voice.
                engine.voice = engine.voices
                    .firstOrNull { it.name == voice.id }
                    ?: throw IllegalStateException("Unknown Android voice ${voice.id}")
            }
            .onRight { language ->
                // Or fallback on the language.
                val localeResult = engine.setLanguage(language.locale)
                if (localeResult < TextToSpeech.LANG_AVAILABLE) {
                    if (localeResult == TextToSpeech.LANG_MISSING_DATA)
                        throw TtsEngine.Exception.LanguageSupportIncomplete(language)
                    else
                        throw TtsEngine.Exception.LanguageNotSupported(language)
                }
            }
    }

    override fun cancel() {
        speakJob?.cancel()
        speakJob = null

        if (init.isCompleted) {
            tryOrLog {
                engine.stop()
            }
        }
    }

    override suspend fun close() {
        cancel()
        engine.shutdown()
        scope.cancel()
    }

    // Engine

    private val init = CompletableDeferred<Unit>()

    private inner class EngineListener : TextToSpeech.OnInitListener, UtteranceProgressListener() {
        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                scope.launch {
                    updateVoices()
                    init.complete(Unit)
                }
            } else {
                listener.onEngineError(TtsEngine.Exception.InitializationFailed())
            }
        }

        override fun onStart(utteranceId: String?) {}

        override fun onStop(utteranceId: String?, interrupted: Boolean) {}

        override fun onDone(utteranceId: String?) {
            utteranceId ?: return
            listener.onDone(utteranceId)
        }

        @Deprecated("Deprecated in the interface", ReplaceWith("onError(utteranceId, -1)"))
        override fun onError(utteranceId: String?) {
            onError(utteranceId, -1)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            utteranceId ?: return
            val error = EngineException(errorCode)
            listener.onUtteranceError(utteranceId, when (error.error) {
                EngineError.Network, EngineError.NetworkTimeout -> TtsEngine.Exception.Network(error)
                else -> TtsEngine.Exception.Other(error)
            })
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            utteranceId ?: return
            listener.onSpeakRange(utteranceId, start until end)
        }
    }

    private fun updateVoices() {
        availableVoices = engine.voices.map { it.toVoice() }
        listener.onAvailableVoicesChange(availableVoices)
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun AndroidVoice.toVoice(): Voice =
    Voice(
        id = name,
        name = null,
        language = Language(locale),
        quality = when (quality) {
            AndroidVoice.QUALITY_VERY_HIGH -> Voice.Quality.Highest
            AndroidVoice.QUALITY_HIGH -> Voice.Quality.High
            AndroidVoice.QUALITY_LOW -> Voice.Quality.Low
            AndroidVoice.QUALITY_VERY_LOW -> Voice.Quality.Lowest
            else -> Voice.Quality.Normal
        },
        requiresNetwork = isNetworkConnectionRequired
    )
