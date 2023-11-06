/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.tts

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try

@ExperimentalReadiumApi
typealias TtsTry<SuccessT> = Try<SuccessT, TtsEngine.Exception>

/**
 * A text-to-speech engine synthesizes text utterances (e.g. sentence).
 *
 * Implement this interface to support third-party engines with [PublicationSpeechSynthesizer].
 */
@ExperimentalReadiumApi
interface TtsEngine : SuspendingCloseable {

    @ExperimentalReadiumApi
    sealed class Exception private constructor(
        override val message: String,
        cause: Throwable? = null
    ) : kotlin.Exception(message, cause) {
        /** Failed to initialize the TTS engine. */
        class InitializationFailed(cause: Throwable? = null) :
            Exception("The TTS engine failed to initialize", cause)

        /** Tried to synthesize an utterance with an unsupported language. */
        class LanguageNotSupported(val language: Language, cause: Throwable? = null) :
            Exception("The language ${language.code} is not supported by the TTS engine", cause)

        /** The selected language is missing downloadable data. */
        class LanguageSupportIncomplete(val language: Language, cause: Throwable? = null) :
            Exception("The language ${language.code} requires additional files by the TTS engine", cause)

        /** Error during network calls. */
        class Network(cause: Throwable? = null) :
            Exception("A network error occurred", cause)

        /** Other engine-specific errors. */
        class Other(override val cause: Throwable) :
            Exception(cause.message ?: "An unknown error occurred", cause)

        companion object {
            fun wrap(e: Throwable): Exception = when (e) {
                is Exception -> e
                else -> Other(e)
            }
        }
    }

    /**
     * TTS engine callbacks.
     */
    @ExperimentalReadiumApi
    interface Listener {
        /**
         * Called when a general engine error occurred.
         */
        fun onEngineError(error: Exception)

        /**
         * Called when the list of available voices is updated.
         */
        fun onAvailableVoicesChange(voices: List<Voice>)
    }

    /**
     * An utterance is an arbitrary text (e.g. sentence) that can be synthesized by the TTS engine.
     *
     * @param text Text to be spoken.
     * @param rateMultiplier Multiplier for the speech rate.
     * @param voiceOrLanguage Either an explicit voice or the language of the text. If a language
     * is provided, the default voice for this language will be used.
     */
    @ExperimentalReadiumApi
    data class Utterance(
        val text: String,
        val rateMultiplier: Double,
        val voiceOrLanguage: Either<Voice, Language>
    ) {
        val language: Language =
            when (val vl = voiceOrLanguage) {
                is Either.Left -> vl.value.language
                is Either.Right -> vl.value
            }
    }

    /**
     * Represents a voice provided by the TTS engine which can speak an utterance.
     *
     * @param id Unique and stable identifier for this voice. Can be used to store and retrieve the
     * voice from the user preferences.
     * @param name Human-friendly name for this voice, when available.
     * @param language Language (and region) this voice belongs to.
     * @param quality Voice quality.
     * @param requiresNetwork Indicates whether using this voice requires an Internet connection.
     */
    @ExperimentalReadiumApi
    data class Voice(
        val id: String,
        val name: String? = null,
        val language: Language,
        val quality: Quality = Quality.Normal,
        val requiresNetwork: Boolean = false,
    ) {
        enum class Quality {
            Lowest, Low, Normal, High, Highest
        }
    }

    /**
     * Synthesizes the given [utterance] and returns its status.
     *
     * [onSpeakRange] is called repeatedly while the engine plays portions (e.g. words) of the
     * utterance.
     *
     * To interrupt the utterance, cancel the parent coroutine job.
     */
    suspend fun speak(
        utterance: Utterance,
        onSpeakRange: (IntRange) -> Unit = { _ -> }
    ): TtsTry<Unit>

    /**
     * Supported range for the speech rate multiplier.
     */
    val rateMultiplierRange: ClosedRange<Double>

    /**
     * List of available synthesizer voices.
     *
     * Implement [Listener.onAvailableVoicesChange] to be aware of changes in the available voices.
     */
    val availableVoices: List<Voice>

    /**
     * Returns the voice with given identifier, if it exists.
     */
    fun voiceWithId(id: String): Voice? =
        availableVoices.firstOrNull { it.id == id }
}
