/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Language

/**
 * A text-to-speech engine synthesizes text utterances (e.g. sentence).
 */
@ExperimentalReadiumApi
interface TtsEngine<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice> : Configurable<S, P>, Closeable {

    interface Preferences<P : Configurable.Preferences<P>> : Configurable.Preferences<P> {

        /**
         * The default language to use when no language information is passed to [speak].
         */
        val language: Language?
    }

    interface Settings : Configurable.Settings {

        /**
         * The default language to use when no language information is passed to [speak].
         */
        val language: Language?

        /**
         * Whether language information in content should be superseded by [language].
         */
        val overrideContentLanguage: Boolean
    }

    interface Voice {

        /**
         * The voice's language.
         */
        val language: Language
    }

    /**
     * Marker interface for the errors that the [TtsEngine] returns.
     */
    interface Error

    /**
     * An id to identify a request to speak.
     */
    @JvmInline
    value class RequestId(val id: String)

    /**
     * TTS engine callbacks.
     */
    interface Listener<E : Error> {

        /**
         * Called when the utterance with the given id starts as perceived by the caller.
         */
        fun onStart(requestId: RequestId)

        /**
         * Called when the [TtsEngine] is about to speak the specified [range] of the utterance with
         * the given id.
         *
         * This callback may not be called if the [TtsEngine] does not provide range information.
         */
        fun onRange(requestId: RequestId, range: IntRange)

        /**
         * Called if the utterance with the given id has been stopped while in progress
         * by a call to [stop].
         */
        fun onInterrupted(requestId: RequestId)

        /**
         * Called when the utterance with the given id has been flushed from the synthesis queue
         * by a call to [stop].
         */
        fun onFlushed(requestId: RequestId)

        /**
         * Called when the utterance with the given id has successfully completed processing.
         */
        fun onDone(requestId: RequestId)

        /**
         * Called when an error has occurred during processing of the utterance with the given id.
         */
        fun onError(requestId: RequestId, error: E)
    }

    /**
     * Sets of voices available with this [TtsEngine].
     */
    val voices: Set<V>

    /**
     * Enqueues a new speak request.
     */
    fun speak(requestId: RequestId, text: String, language: Language?)

    /**
     * Stops the [TtsEngine].
     */
    fun stop()

    /**
     * Sets a new listener or removes the current one.
     */
    fun setListener(listener: Listener<E>?)
}
