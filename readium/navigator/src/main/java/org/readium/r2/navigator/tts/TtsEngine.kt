/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.tts

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.SuspendingCloseable
import java.util.*

interface TtsEngine : SuspendingCloseable {

    sealed class Exception private constructor(
        override val message: String,
        cause: Throwable? = null
    ) : kotlin.Exception(message, cause) {
        /** Failed to initialize the TTS engine. */
        class InitializationFailed(cause: Throwable? = null) : Exception("The TTS engine failed to initialize", cause)
        class LanguageNotSupported(val language: Language, cause: Throwable? = null) : Exception("The language ${language.code} is not supported by the TTS engine", cause)
        class LanguageSupportIncomplete(val language: Language, cause: Throwable? = null) : Exception("The language ${language.code} requires additional files by the TTS engine", cause)
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
        val defaultLanguage: Language? = null,
        val voice: Voice? = null,
        val rate: Double = 1.0,
    )

    data class ConfigurationConstraints(
        val rateRange: ClosedRange<Double> = 1.0..1.0,
        val availableVoices: List<Voice> = emptyList()
    )

    data class Voice(
        val identifier: String,
        val name: String? = null,
        val language: Language,
        val quality: Quality = Quality.Normal,
        val requiresNetwork: Boolean = false,
    ) {
        enum class Quality {
            Lowest, Low, Normal, High, Highest
        }
    }

    val config: StateFlow<Configuration>
    val configConstraints: StateFlow<ConfigurationConstraints>
    fun setConfig(config: Configuration): Configuration

    data class Utterance(
        val text: String,
        val locator: Locator,
        val language: Language?
    )

    fun speak(utterance: Utterance)
    fun stop()
}