/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.tts.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.*
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice as AndroidVoice
import android.speech.tts.Voice.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.navigator.media.tts.TtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.Language

/*
 * On some Android implementations (i.e. on Oppo A9 2020 running Android 11),
 * the TextToSpeech instance is often disconnected from the underlying service when the playback
 * is paused and the app moves to the background. So we try to reset the TextToSpeech before
 * actually returning an error. In the meantime, requests to the engine are queued
 * into [pendingRequests].
 */

/**
 * Default [TtsEngine] implementation using Android's native text to speech engine.
 */
@ExperimentalReadiumApi
public class AndroidTtsEngine private constructor(
    private val context: Context,
    engine: TextToSpeech,
    private val settingsResolver: SettingsResolver,
    private val voiceSelector: VoiceSelector,
    override val voices: Set<Voice>,
    initialPreferences: AndroidTtsPreferences,
) : TtsEngine<
    AndroidTtsSettings,
    AndroidTtsPreferences,
    AndroidTtsEngine.Error,
    AndroidTtsEngine.Voice
    > {

    public companion object {

        public suspend operator fun invoke(
            context: Context,
            settingsResolver: SettingsResolver,
            voiceSelector: VoiceSelector,
            initialPreferences: AndroidTtsPreferences,
        ): AndroidTtsEngine? {
            val textToSpeech = initializeTextToSpeech(context)
                ?: return null

            val voices = tryOrNull { textToSpeech.voices } // throws on Nexus 4
                ?.map { it.toTtsEngineVoice() }
                ?.toSet()
                .orEmpty()

            return AndroidTtsEngine(
                context,
                textToSpeech,
                settingsResolver,
                voiceSelector,
                voices,
                initialPreferences
            )
        }

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
    }

    public fun interface SettingsResolver {

        /**
         * Computes a set of engine settings from the engine preferences.
         */
        public fun settings(preferences: AndroidTtsPreferences): AndroidTtsSettings
    }

    public fun interface VoiceSelector {

        /**
         * Selects a voice for the given [language].
         */
        public fun voice(language: Language?, availableVoices: Set<Voice>): Voice?
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error? = null,
    ) : TtsEngine.Error {

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
     * @param id Unique and stable identifier for this voice
     * @param language Language (and region) this voice belongs to.
     * @param quality Voice quality.
     * @param requiresNetwork Indicates whether using this voice requires an Internet connection.
     */
    public data class Voice(
        val id: Id,
        override val language: Language,
        val quality: Quality = Quality.Normal,
        val requiresNetwork: Boolean = false,
    ) : TtsEngine.Voice {

        @kotlinx.serialization.Serializable
        @JvmInline
        public value class Id(public val value: String)

        public enum class Quality {
            Lowest,
            Low,
            Normal,
            High,
            Highest,
        }
    }

    private data class Request(
        val id: TtsEngine.RequestId,
        val text: String,
        val language: Language?,
    )

    private sealed class State {

        data class EngineAvailable(
            val engine: TextToSpeech,
        ) : State()

        data class WaitingForService(
            val pendingRequests: MutableList<Request> = mutableListOf(),
        ) : State()

        data class Failure(
            val error: AndroidTtsEngine.Error,
        ) : State()
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val _settings: MutableStateFlow<AndroidTtsSettings> =
        MutableStateFlow(settingsResolver.settings(initialPreferences))
            .apply { engine.setupPitchAndSpeed(value) }

    private var utteranceListener: TtsEngine.Listener<Error>? =
        null

    private var state: State =
        State.EngineAvailable(engine)

    private var isClosed: Boolean =
        false

    override val settings: StateFlow<AndroidTtsSettings> =
        _settings.asStateFlow()

    override fun submitPreferences(preferences: AndroidTtsPreferences) {
        val newSettings = settingsResolver.settings(preferences)
        _settings.value = newSettings
        (state as? State.EngineAvailable)
            ?.engine?.setupPitchAndSpeed(newSettings)
    }

    override fun setListener(
        listener: TtsEngine.Listener<Error>?,
    ) {
        utteranceListener = listener
        (state as? State.EngineAvailable)
            ?.let { setupListener(it.engine) }
    }

    override fun speak(
        requestId: TtsEngine.RequestId,
        text: String,
        language: Language?,
    ) {
        check(!isClosed) { "Engine is closed." }
        val request = Request(requestId, text, language)

        when (val stateNow = state) {
            is State.WaitingForService -> {
                stateNow.pendingRequests.add(request)
            }
            is State.Failure -> {
                tryReconnect(request)
            }
            is State.EngineAvailable -> {
                if (!doSpeak(stateNow.engine, request)) {
                    cleanEngine(stateNow.engine)
                    tryReconnect(request)
                }
            }
        }
    }

    override fun stop() {
        when (val stateNow = state) {
            is State.EngineAvailable -> {
                stateNow.engine.stop()
            }
            is State.Failure -> {
                // Do nothing
            }
            is State.WaitingForService -> {
                for (request in stateNow.pendingRequests) {
                    utteranceListener?.onFlushed(request.id)
                }
                stateNow.pendingRequests.clear()
            }
        }
    }

    override fun close() {
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

    private fun doSpeak(
        engine: TextToSpeech,
        request: Request,
    ): Boolean {
        return engine.setupVoice(settings.value, request.id, request.language, voices) &&
            (engine.speak(request.text, QUEUE_ADD, null, request.id.value) == SUCCESS)
    }

    private fun setupListener(engine: TextToSpeech) {
        if (utteranceListener == null) {
            engine.setOnUtteranceProgressListener(null)
        } else {
            engine.setOnUtteranceProgressListener(UtteranceListener(utteranceListener))
        }
    }

    private fun onReconnectionSucceeded(engine: TextToSpeech) {
        val previousState = state as State.WaitingForService
        setupListener(engine)
        engine.setupPitchAndSpeed(_settings.value)
        state = State.EngineAvailable(engine)
        if (isClosed) {
            engine.shutdown()
        } else {
            for (request in previousState.pendingRequests) {
                doSpeak(engine, request)
            }
        }
    }

    private fun onReconnectionFailed() {
        val previousState = state as State.WaitingForService
        val error = Error.Service
        state = State.Failure(error)

        for (request in previousState.pendingRequests) {
            utteranceListener?.onError(request.id, error)
        }
    }

    private fun tryReconnect(request: Request) {
        state = State.WaitingForService(mutableListOf(request))
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

    private fun TextToSpeech.setupPitchAndSpeed(settings: AndroidTtsSettings) {
        setSpeechRate(settings.speed.toFloat())
        setPitch(settings.pitch.toFloat())
    }

    private fun TextToSpeech.setupVoice(
        settings: AndroidTtsSettings,
        id: TtsEngine.RequestId,
        utteranceLanguage: Language?,
        voices: Set<Voice>,
    ): Boolean {
        val language = utteranceLanguage
            .takeUnless { settings.overrideContentLanguage }
            // We take utterance language if data are missing but not if the language is not supported
            ?.takeIf { isLanguageAvailable(it.locale) != LANG_NOT_SUPPORTED }
            ?: settings.language
                .takeIf { isLanguageAvailable(it.locale) != LANG_NOT_SUPPORTED }
            ?: defaultVoice?.locale?.let { Language(it) }

        if (language == null) {
            // We don't know what to do.
            utteranceListener?.onError(id, Error.Unknown)
            return false
        }

        if (isLanguageAvailable(language.locale) < LANG_AVAILABLE) {
            utteranceListener?.onError(id, Error.LanguageMissingData(language))
            return false
        }

        val preferredVoiceWithRegion =
            settings.voices[language]
                ?.let { voiceForName(it.value) }

        val preferredVoiceWithoutRegion =
            settings.voices[language.removeRegion()]
                ?.let { voiceForName(it.value) }

        val voice = preferredVoiceWithRegion
            ?: preferredVoiceWithoutRegion
            ?: run {
                voiceSelector
                    .voice(language, voices)
                    ?.let { voiceForName(it.id.value) }
            }

        voice
            ?.let { this.voice = it }
            ?: run { this.language = language.locale }

        return true
    }

    private fun TextToSpeech.voiceForName(name: String) =
        voices.firstOrNull { it.name == name }

    private class UtteranceListener(
        private val listener: TtsEngine.Listener<Error>?,
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

        @Deprecated(
            "Deprecated in the interface",
            ReplaceWith("onError(utteranceId, -1)"),
            level = DeprecationLevel.ERROR
        )
        override fun onError(utteranceId: String) {
            onError(utteranceId, -1)
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            listener?.onError(
                TtsEngine.RequestId(utteranceId),
                Error.fromNativeError(errorCode)
            )
        }

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            listener?.onRange(TtsEngine.RequestId(utteranceId), start until end)
        }
    }
}
