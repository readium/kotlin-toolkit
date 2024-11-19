/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Language

/**
 * A text-to-speech engine synthesizes text utterances (e.g. sentence).
 */
@ExperimentalReadiumApi
public interface TtsEngine<
    S : TtsEngine.Settings,
    P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error,
    V : TtsEngine.Voice,
    > : Configurable<S, P>, Closeable {

    public interface Preferences<P : Configurable.Preferences<P>> : Configurable.Preferences<P> {

        /**
         * The default language to use when no language information is passed to [speak].
         */
        public val language: Language?
    }

    public interface Settings : Configurable.Settings {

        /**
         * The default language to use when no language information is passed to [speak].
         */
        public val language: Language?

        /**
         * Whether language information in content should be superseded by [language].
         */
        public val overrideContentLanguage: Boolean
    }

    public interface Voice {

        /**
         * The voice's language.
         */
        public val language: Language
    }

    /**
     * Marker interface for the errors that the [TtsEngine] returns.
     */
    public interface Error : org.readium.r2.shared.util.Error

    /**
     * An id to identify a request to speak.
     */
    @JvmInline
    public value class RequestId(public val value: String)

    /**
     * TTS engine callbacks.
     */
    public interface Listener<E : Error> {

        /**
         * Called when the utterance with the given id starts as perceived by the caller.
         */
        public fun onStart(requestId: RequestId)

        /**
         * Called when the [TtsEngine] is about to speak the specified [range] of the utterance with
         * the given id.
         *
         * This callback may not be called if the [TtsEngine] does not provide range information.
         */
        public fun onRange(requestId: RequestId, range: IntRange)

        /**
         * Called if the utterance with the given id has been stopped while in progress
         * by a call to [stop].
         */
        public fun onInterrupted(requestId: RequestId)

        /**
         * Called when the utterance with the given id has been flushed from the synthesis queue
         * by a call to [stop].
         */
        public fun onFlushed(requestId: RequestId)

        /**
         * Called when the utterance with the given id has successfully completed processing.
         */
        public fun onDone(requestId: RequestId)

        /**
         * Called when an error has occurred during processing of the utterance with the given id.
         */
        public fun onError(requestId: RequestId, error: E)
    }

    /**
     * Sets of voices available with this [TtsEngine].
     */
    public val voices: Set<V>

    /**
     * Enqueues a new speak request.
     */
    public fun speak(requestId: RequestId, text: String, language: Language?)

    /**
     * Stops the [TtsEngine].
     */
    public fun stop()

    /**
     * Sets a new listener or removes the current one.
     */
    public fun setListener(listener: Listener<E>?)
}
