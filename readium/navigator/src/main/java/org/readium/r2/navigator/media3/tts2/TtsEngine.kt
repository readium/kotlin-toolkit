/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Language

/**
 * A text-to-speech engine synthesizes text utterances (e.g. sentence).
 */
@ExperimentalReadiumApi
interface TtsEngine<S : TtsSettings, P : TtsPreferences<P>> : Configurable<S, P>, Closeable {

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

        fun onStart(id: String)

        fun onRange(id: String, range: IntRange)

        fun onInterrupted(id: String)

        fun onFlushed(id: String)

        fun onDone(id: String)

        fun onError(id: String, error: Exception)
    }

    fun speak(utterance: TtsUtterance, requestId: String)

    fun stop()

    fun setListener(listener: Listener?)
}
