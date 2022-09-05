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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import org.readium.r2.navigator.tts.TtsEngine.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.Try
import java.util.*
import kotlin.Exception
import kotlin.coroutines.resume
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Utterances to be synthesized, in order of [speak] calls.
     */
    private val tasks = Channel<UtteranceTask>(Channel.BUFFERED)

    /** Future completed when the [engine] is fully initialized. */
    private val init = CompletableDeferred<Unit>()

    init {
        scope.launch {
            init.await()

            for (task in tasks) {
                ensureActive()
                task.run()
            }
        }
    }

    override val rateMultiplierRange: ClosedRange<Double> = 0.1..4.0

    override var availableVoices: List<Voice> = emptyList()
        private set(value) {
            field = value
            listener.onAvailableVoicesChange(value)
        }

    override suspend fun close() {
        scope.cancel()
        tasks.cancel()
        engine.shutdown()
    }

    override suspend fun speak(
        utterance: Utterance,
        onSpeakRange: (IntRange) -> Unit
    ): TtsTry<Unit> =
        suspendCancellableCoroutine { cont ->
            val result = tasks.trySend(UtteranceTask(
                utterance = utterance,
                continuation = cont,
                onSpeakRange = onSpeakRange
            ))

            result.onFailure {
                listener.onEngineError(
                    TtsEngine.Exception.Other(IllegalStateException("Failed to schedule a new utterance task")))
            }
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

    // Engine

    /** Underlying Android [TextToSpeech] engine. */
    private val engine = TextToSpeech(context, EngineInitListener())

    private inner class EngineInitListener : TextToSpeech.OnInitListener {
        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                scope.launch {
                    tryOrLog {
                        availableVoices = engine.voices.map { it.toVoice() }
                    }
                    init.complete(Unit)
                }
            } else {
                listener.onEngineError(TtsEngine.Exception.InitializationFailed())
            }
        }
    }

    /**
     * Holds a single utterance to be synthesized and the continuation for the [speak] call.
     */
    private inner class UtteranceTask(
        val utterance: Utterance,
        val continuation: CancellableContinuation<TtsTry<Unit>>,
        val onSpeakRange: (IntRange) -> Unit,
    ) {
        fun run() {
            if (!continuation.isActive) return

            // Interrupt the engine when the task is cancelled.
            continuation.invokeOnCancellation {
                tryOrLog {
                    engine.stop()
                    engine.setOnUtteranceProgressListener(null)
                }
            }

            try {
                val id = UUID.randomUUID().toString()
                engine.setup()
                engine.setOnUtteranceProgressListener(Listener(id))
                engine.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, null, id)
            } catch (e: kotlin.Exception) {
                finish(TtsEngine.Exception.wrap(e))
            }
        }

        /**
         * Terminates this task.
         */
        private fun finish(error: TtsEngine.Exception? = null) {
            continuation.resume(
                error?.let { Try.failure(error) }
                    ?: Try.success(Unit)
            )
        }

        /**
         * Setups the [engine] using the [utterance]'s configuration.
         */
        private fun TextToSpeech.setup() {
            setSpeechRate(utterance.rateMultiplier.toFloat())

            utterance.voiceOrLanguage
                .onLeft { voice ->
                    // Setup the user selected voice.
                    engine.voice = engine.voices
                        .firstOrNull { it.name == voice.id }
                        ?: throw IllegalStateException("Unknown Android voice: ${voice.id}")
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

        inner class Listener(val id: String) : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                require(utteranceId == id)
                finish()
            }

            override fun onDone(utteranceId: String?) {
                require(utteranceId == id)
                finish()
            }

            @Deprecated("Deprecated in the interface", ReplaceWith("onError(utteranceId, -1)"))
            override fun onError(utteranceId: String?) {
                onError(utteranceId, -1)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                require(utteranceId == id)

                val error = EngineException(errorCode)
                finish(when (error.error) {
                    EngineError.Network, EngineError.NetworkTimeout ->
                        TtsEngine.Exception.Network(error)
                    EngineError.NotInstalledYet ->
                        TtsEngine.Exception.LanguageSupportIncomplete(utterance.language, cause = error)
                    else -> TtsEngine.Exception.Other(error)
                })
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                require(utteranceId == id)
                onSpeakRange(start until end)
            }
        }
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
