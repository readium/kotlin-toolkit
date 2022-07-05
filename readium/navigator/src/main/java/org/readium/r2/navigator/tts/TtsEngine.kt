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

/**
 * A text-to-speech engine synthesizes text utterances (e.g. sentence).
 *
 * Implement this interface to support third-party engines with [TtsDirector].
 */
@ExperimentalReadiumApi
interface TtsEngine : SuspendingCloseable {

    @ExperimentalReadiumApi
    sealed class Exception private constructor(
        override val message: String,
        cause: Throwable? = null
    ) : kotlin.Exception(message, cause) {
        /** Failed to initialize the TTS engine. */
        class InitializationFailed(cause: Throwable? = null)
            : Exception("The TTS engine failed to initialize", cause)

        /** Tried to synthesize an utterance with an unsupported language. */
        class LanguageNotSupported(val language: Language, cause: Throwable? = null)
            : Exception("The language ${language.code} is not supported by the TTS engine", cause)

        /** The selected language is missing downloadable data. */
        class LanguageSupportIncomplete(val language: Language, cause: Throwable? = null)
            : Exception("The language ${language.code} requires additional files by the TTS engine", cause)

        /** Error during network calls. */
        class Network(cause: Throwable? = null)
            : Exception("A network error occurred", cause)

        /** Other engine-specific errors. */
        class Other(override val cause: Throwable)
            : Exception(cause.message ?: "An unknown error occurred", cause)

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
         * Called when the engine speaks a portion of an utterance (e.g. a word).
         */
        fun onSpeakRange(utteranceId: String, range: IntRange)

        /**
         * Called when an utterance was successfully synthesized.
         *
         * This won't be called if the utterance was cancelled or an error occured.
         */
        fun onDone(utteranceId: String)

        /**
         * Called when a general engine error occurred.
         */
        fun onEngineError(error: Exception)

        /**
         * Called when an error occurred while synthesizing an utterance.
         */
        fun onUtteranceError(utteranceId: String, error: Exception)

        /**
         * Called when the list of available voices is updated.
         */
        fun onAvailableVoicesChange(voices: List<Voice>)
    }

    /**
     * An utterance is an arbitrary text (e.g. sentence) that can be synthesized by the TTS engine.
     *
     * @param id Unique identifier for this utterance, in the context of the caller.
     * @param text Text to be spoken.
     * @param rate Speed of the voice.
     * @param voiceOrLanguage Either an explicit voice or the language of the text. If a language
     * is provided, the default voice for this language will be used.
     */
    @ExperimentalReadiumApi
    data class Utterance(
        val id: String,
        val text: String,
        val rate: Double,
        val voiceOrLanguage: Either<Voice, Language>
    )

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
     * Requests to speak the given utterance.
     */
    fun speak(utterance: Utterance)

    /**
     * Cancels the currently spoken utterance.
     */
    fun cancel()

    val rateRange: ClosedRange<Double>
    val availableVoices: List<Voice>

    /**
     * Returns the voice with given identifier, if it exists.
     */
    fun voiceWithId(id: String): Voice? =
        availableVoices.firstOrNull { it.id == id }
}