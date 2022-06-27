/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.tts

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.SuspendingCloseable
import java.util.Locale

interface TtsEngine : SuspendingCloseable {

    sealed class Exception private constructor(
        override val message: String,
        cause: Throwable? = null
    ) : kotlin.Exception(message, cause) {
        /** Failed to initialize the TTS engine. */
        class InitializationFailed(cause: Throwable? = null) : Exception("The TTS engine failed to initialize", cause)
        class LanguageNotSupported(val locale: Locale, cause: Throwable? = null) : Exception("The language ${locale.toLanguageTag()} is not supported by the TTS engine", cause)
        class LanguageSupportIncomplete(val locale: Locale, cause: Throwable? = null) : Exception("The language ${locale.toLanguageTag()} requires additional files by the TTS engine", cause)
        class Network(cause: Throwable? = null) : Exception("A network error occurred", cause)
        class Other(override val cause: Throwable) : Exception(cause.message ?: "An unknown error occurred", cause)
    }

    interface Listener {
        fun onSpeakRangeAt(locator: Locator, utterance: Utterance)
        fun onStop()
        fun onEngineError(error: Exception)
        fun onUtteranceError(utterance: Utterance, error: Exception)
    }

    data class Configuration(
        val defaultLocale: Locale? = null,
        val voice: Voice? = null,
        val rate: Double = 1.0,
    )

    data class Voice(
        val identifier: String,
        val name: String,
        val locale: Locale,
        val quality: Quality = Quality.Normal,
        val requiresNetwork: Boolean = false,
    ) {
        enum class Quality {
            Lowest, Low, Normal, High, Highest
        }
    }

    val config: StateFlow<Configuration>
    fun setConfig(config: Configuration): Configuration

    val availableLocales: StateFlow<Set<Locale>>

    val availableVoices: StateFlow<Set<Voice>>

    fun voiceWithIdentifier(identifier: String): Voice?

    data class Utterance(
        val text: String,
        val locator: Locator,
        val language: Locale?
    )

    fun speak(utterance: Utterance)
    fun stop()
}