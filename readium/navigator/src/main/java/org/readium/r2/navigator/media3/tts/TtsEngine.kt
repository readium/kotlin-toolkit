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

        val language: Language?
    }

    interface Settings : Configurable.Settings {

        val language: Language?
    }

    interface Voice {

        val language: Language
    }

    interface Error

    /**
     * TTS engine callbacks.
     */
    interface Listener<E : Error> {

        fun onStart(requestId: String)

        fun onRange(requestId: String, range: IntRange)

        fun onInterrupted(requestId: String)

        fun onFlushed(requestId: String)

        fun onDone(requestId: String)

        fun onError(requestId: String, error: E)
    }

    val voices: Set<V>

    fun speak(requestId: String, text: String, language: Language?)

    fun stop()

    fun setListener(listener: Listener<E>?)
}
