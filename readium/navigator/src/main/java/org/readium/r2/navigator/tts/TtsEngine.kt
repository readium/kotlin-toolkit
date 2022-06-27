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

typealias TtsEngineFactory = (listener: TtsEngine.Listener) -> TtsEngine

interface TtsEngine : SuspendingCloseable {

    interface Listener {
        fun onSpeakRangeAt(locator: Locator, utterance: Utterance)
        fun onStop()
        fun onError(error: Exception)
    }

    data class Configuration(
        val defaultLocale: Locale? = null,
        val rate: Double = 1.0,
    )

    data class Utterance(
        val text: String,
        val locator: Locator,
        val language: Locale?
    )

    val config: StateFlow<Configuration>

    suspend fun setConfig(config: Configuration): Configuration

    // Can throw.
    suspend fun speak(utterance: Utterance)

    // Can throw.
    suspend fun stop()
}