/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class, ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR_INVALID_REQUEST
import android.speech.tts.TextToSpeech.ERROR_NETWORK
import android.speech.tts.TextToSpeech.ERROR_NETWORK_TIMEOUT
import android.speech.tts.TextToSpeech.ERROR_NOT_INSTALLED_YET
import android.speech.tts.TextToSpeech.ERROR_OUTPUT
import android.speech.tts.TextToSpeech.ERROR_SERVICE
import android.speech.tts.TextToSpeech.ERROR_SYNTHESIS
import android.speech.tts.TextToSpeech.Engine
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.TextToSpeech.QUEUE_ADD
import android.speech.tts.TextToSpeech.SUCCESS
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice as AndroidVoice
import android.speech.tts.Voice.QUALITY_HIGH
import android.speech.tts.Voice.QUALITY_LOW
import android.speech.tts.Voice.QUALITY_NORMAL
import android.speech.tts.Voice.QUALITY_VERY_HIGH
import android.speech.tts.Voice.QUALITY_VERY_LOW
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
public class SystemTtsEngineProvider(
    private val context: Context,
    private val maxConnectionRetries: Int = 3,
) : TtsEngineProvider<SystemTtsEngine.Voice, SystemTtsEngine.Error> {

    override suspend fun createEngineFactory(): TtsEngineFactory<SystemTtsEngine.Voice, SystemTtsEngine.Error> =
        tryCreateEngineFactory(maxConnectionRetries) ?: NullTtsEngineFactory()

    private suspend fun tryCreateEngineFactory(maxRetries: Int): SystemTtsEngineFactory? {
        suspend fun onFailure(): SystemTtsEngineFactory? =
            if (maxRetries == 0) {
                null
            } else {
                tryCreateEngineFactory(maxRetries - 1)
            }

        val textToSpeech = initializeTextToSpeech(context)
            ?: return onFailure()

        // Listing voices is not reliable.
        val voices = tryOrNull { textToSpeech.voices } // throws on Nexus 4
            ?.filterNotNull()
            ?.takeUnless { it.isEmpty() }
            ?.associate { it.toTtsEngineVoice() to it }
            ?: return onFailure()

        return SystemTtsEngineFactory(
            context = context,
            textToSpeech = textToSpeech,
            fullVoices = voices,
            maxConnectionRetries = maxConnectionRetries
        )
    }
}

@ExperimentalReadiumApi
public class SystemTtsEngineFactory internal constructor(
    private val context: Context,
    private val textToSpeech: TextToSpeech,
    private val fullVoices: Map<SystemTtsEngine.Voice, AndroidVoice>,
    private val maxConnectionRetries: Int,
) : TtsEngineFactory<SystemTtsEngine.Voice, SystemTtsEngine.Error> {

    override val voices: Set<SystemTtsEngine.Voice> =
        fullVoices.keys

    override fun createPlaybackEngine(
        voiceId: TtsVoice.Id,
        utterances: List<String>,
        listener: PlaybackEngine.Listener<TtsEngineProgress, SystemTtsEngine.Error>,
    ): PlaybackEngine {
        val voice = fullVoices.keys.firstOrNull { it.id == voiceId }
        requireNotNull(voice) { "Voice id $voiceId doesn't match any voice in $voices." }

        return SystemTtsEngine(
            context = context,
            engine = textToSpeech,
            listener = listener,
            systemVoice = fullVoices[voice]!!,
            utterances = utterances,
            maxConnectionRetries = maxConnectionRetries
        )
    }
}

/*
 * On some Android implementations (i.e. on Oppo A9 2020 running Android 11),
 * the TextToSpeech instance is often disconnected from the underlying service when the playback
 * is paused and the app moves to the background. So we try to reset the TextToSpeech before
 * actually returning an error. In the meantime, requests to the engine are stored in the adapter
 * state.
 */

/**
 * Default [PlaybackEngine] implementation using Android's native text to speech engine.
 */
@ExperimentalReadiumApi
public class SystemTtsEngine internal constructor(
    private val context: Context,
    engine: TextToSpeech,
    private val listener: PlaybackEngine.Listener<TtsEngineProgress, Error>,
    private val systemVoice: AndroidVoice,
    private val utterances: List<String>,
    private val maxConnectionRetries: Int,
) : PlaybackEngine {

    public companion object {

        /**
         * Starts the activity to install additional voice data.
         */
        @SuppressLint("QueryPermissionsNeeded")
        public fun requestInstallVoice(context: Context) {
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
                    context.packageManager.queryIntentActivities(intent, 0)
                }

            if (availableActivities.isNotEmpty()) {
                context.startActivity(intent)
            }
        }
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error? = null,
    ) : org.readium.r2.shared.util.Error {

        /** Denotes a generic operation failure. */
        public data object Unknown : Error("An unknown error occurred.")

        /** Denotes a failure caused by an invalid request. */
        public data object InvalidRequest : Error("Invalid request")

        /** Denotes a failure caused by a network connectivity problems. */
        public data object Network : Error("A network error occurred.")

        /** Denotes a failure caused by network timeout. */
        public data object NetworkTimeout : Error("Network timeout")

        /** Denotes a failure caused by an unfinished download of the voice data. */
        public data object NotInstalledYet : Error("Voice not installed yet.")

        /** Denotes a failure related to the output (audio device or a file). */
        public data object Output : Error("An error related to the output occurred.")

        /** Denotes a failure of a TTS service. */
        public data object Service : Error("An error occurred with the TTS service.")

        /** Denotes a failure of a TTS engine to synthesize the given input. */
        public data object Synthesis : Error("Synthesis failed.")

        /**
         * Denotes the language data is missing.
         *
         * You can open the Android settings to install the missing data with:
         * AndroidTtsEngine.requestInstallVoice(context)
         */
        public data class LanguageMissingData(val language: Language) :
            Error("Language data is missing.")

        /**
         * Android's TTS error code.
         * See https://developer.android.com/reference/android/speech/tts/TextToSpeech#ERROR
         */
        public companion object {
            internal fun fromNativeError(code: Int): Error =
                when (code) {
                    ERROR_INVALID_REQUEST -> InvalidRequest
                    ERROR_NETWORK -> Network
                    ERROR_NETWORK_TIMEOUT -> NetworkTimeout
                    ERROR_NOT_INSTALLED_YET -> NotInstalledYet
                    ERROR_OUTPUT -> Output
                    ERROR_SERVICE -> Service
                    ERROR_SYNTHESIS -> Synthesis
                    else -> Unknown
                }
        }
    }

    /**
     * Represents a voice provided by the TTS engine which can speak an utterance.
     *
     * @param name Voice name
     * @param language Language (and region) this voice belongs to.
     * @param quality Voice quality.
     * @param requiresNetwork Indicates whether using this voice requires an Internet connection.
     */
    public data class Voice(
        val name: String,
        val language: Language,
        val quality: Quality = Quality.Normal,
        val requiresNetwork: Boolean = false,
    ) : TtsVoice {

        override val id: TtsVoice.Id =
            TtsVoice.Id("${SystemTtsEngine::class.qualifiedName}-$name}")

        override val languages: Set<Language> =
            setOf(language)

        public enum class Quality {
            Lowest,
            Low,
            Normal,
            High,
            Highest,
        }
    }

    private data class Request(
        val id: Id,
        val text: String,
    ) {

        @JvmInline
        value class Id(val value: String)
    }

    private sealed class State {

        data class EngineAvailable(
            val engine: TextToSpeech,
        ) : State()

        data class WaitingForService(
            var pendingRequest: Request?,
        ) : State()

        data class Failure(
            val error: Error,
        ) : State()
    }

    private val coroutineScope: CoroutineScope = MainScope()

    private var state: State = State.EngineAvailable(engine)

    private var isClosed: Boolean = false

    override var speed: Double = 1.0

    override var pitch: Double = 1.0

    override var itemToPlay: Int = 0

    init {
        setupListener(engine)
    }

    override fun start() {
        listener.onStartRequested(PlaybackEngine.PlaybackState.Playing)
        doStart(itemToPlay)
    }

    override fun resume() {
        doStart(itemToPlay)
    }

    private fun doStart(utteranceIndex: Int) {
        check(!isClosed) { "Engine is closed." }

        val id = Request.Id(UUID.randomUUID().toString())
        val text = checkNotNull(utterances)[utteranceIndex]
        val request = Request(id, text)

        when (val stateNow = state) {
            is State.WaitingForService -> {
                stateNow.pendingRequest = request
            }
            is State.Failure -> {
                tryReconnect(request)
            }
            is State.EngineAvailable -> {
                if (!speak(stateNow.engine, request)) {
                    cleanEngine(stateNow.engine)
                    tryReconnect(request)
                }
            }
        }
    }

    public override fun stop() {
        doStop()
    }

    override fun pause() {
        doStop()
    }

    private fun doStop() {
        when (val stateNow = state) {
            is State.EngineAvailable -> {
                stateNow.engine.stop()
            }
            is State.Failure -> {
                // Do nothing
            }
            is State.WaitingForService -> {
                stateNow.pendingRequest = null
            }
        }
    }

    public override fun release() {
        if (isClosed) {
            return
        }

        isClosed = true
        coroutineScope.cancel()

        when (val stateNow = state) {
            is State.EngineAvailable -> {
                cleanEngine(stateNow.engine)
            }
            is State.Failure -> {
                // Do nothing
            }
            is State.WaitingForService -> {
                // Do nothing
            }
        }
    }

    private fun speak(
        engine: TextToSpeech,
        request: Request,
    ): Boolean {
        return engine.setupPitchAndSpeed() &&
            engine.setupVoice() &&
            engine.speak(request.text, QUEUE_ADD, null, request.id.value) == SUCCESS
    }

    private fun setupListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(UtteranceListener(listener))
    }

    private fun onReconnectionSucceeded(engine: TextToSpeech) {
        val previousState = state as State.WaitingForService
        setupListener(engine)
        state = State.EngineAvailable(engine)
        if (isClosed) {
            engine.shutdown()
        } else {
            previousState.pendingRequest?.let { speak(engine, it) }
        }
    }

    private fun onReconnectionFailed() {
        val error = Error.Service
        state = State.Failure(error)
        // listener.onError(error)
    }

    private fun tryReconnect(request: Request) {
        state = State.WaitingForService(request)
        coroutineScope.launch {
            initializeTextToSpeech(context)
                ?.let { onReconnectionSucceeded(it) }
                ?: onReconnectionFailed()
        }
    }

    private fun cleanEngine(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(null)
        engine.shutdown()
    }

    private fun TextToSpeech.setupPitchAndSpeed(): Boolean {
        if (setSpeechRate(speed.toFloat()) != SUCCESS) {
            return false
        }

        if (setPitch(pitch.toFloat()) != SUCCESS) {
            return false
        }

        return true
    }

    private fun TextToSpeech.setupVoice(): Boolean {
        return setVoice(systemVoice) == SUCCESS
    }

    private class UtteranceListener(
        private val listener: PlaybackEngine.Listener<TtsEngineProgress, Error>,
    ) : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
        }

        override fun onStop(utteranceId: String, interrupted: Boolean) {
        }

        override fun onDone(utteranceId: String) {
            listener.onPlaybackCompleted()
        }

        @Deprecated(
            "Deprecated in the interface",
            ReplaceWith("onError(utteranceId, -1)"),
            level = DeprecationLevel.ERROR
        )
        override fun onError(utteranceId: String) {
            onError(utteranceId, -1)
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            // listener.onError(Error.fromNativeError(errorCode))
        }

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            listener.onPlaybackProgressed(TtsEngineProgress(start until end))
        }
    }
}

private fun AndroidVoice.toTtsEngineVoice() =
    SystemTtsEngine.Voice(
        name = name,
        language = Language(locale),
        quality = when (quality) {
            QUALITY_VERY_HIGH -> SystemTtsEngine.Voice.Quality.Highest
            QUALITY_HIGH -> SystemTtsEngine.Voice.Quality.High
            QUALITY_NORMAL -> SystemTtsEngine.Voice.Quality.Normal
            QUALITY_LOW -> SystemTtsEngine.Voice.Quality.Low
            QUALITY_VERY_LOW -> SystemTtsEngine.Voice.Quality.Lowest
            else -> throw IllegalStateException("Unexpected voice quality.")
        },
        requiresNetwork = isNetworkConnectionRequired
    )

private suspend fun initializeTextToSpeech(
    context: Context,
): TextToSpeech? {
    val init = CompletableDeferred<Boolean>()

    val initListener = OnInitListener { status ->
        init.complete(status == SUCCESS)
    }
    val engine = TextToSpeech(context, initListener)
    return if (init.await()) engine else null
}
