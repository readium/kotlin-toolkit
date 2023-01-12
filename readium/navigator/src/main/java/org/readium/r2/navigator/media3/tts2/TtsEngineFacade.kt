/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import java.util.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
@OptIn(ExperimentalCoroutinesApi::class)
internal class TtsEngineFacade<S : TtsEngine.Settings, P : TtsEngine.Preferences<P>,
    E : TtsEngine.Error, V : TtsEngine.Voice>(
    private val engine: TtsEngine<S, P, E, V>
) : Configurable<S, P> by engine {

    init {
        val listener = TtsEngineListener()
        engine.setListener(listener)
    }

    private var currentTask: UtteranceTask<E>? = null

    val voices: Set<V>
        get() = engine.voices

    suspend fun speak(text: String, language: Language?, onRange: (IntRange) -> Unit): E? =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { engine.stop() }
            val id = UUID.randomUUID().toString()
            currentTask?.continuation?.cancel()
            currentTask = UtteranceTask(id, continuation, onRange)
            engine.speak(id, text, language)
        }

    fun close() {
        currentTask?.continuation?.cancel()
        engine.close()
    }

    private data class UtteranceTask<E : TtsEngine.Error>(
        val requestId: String,
        val continuation: CancellableContinuation<E?>,
        val onRange: (IntRange) -> Unit
    )

    private inner class TtsEngineListener : TtsEngine.Listener<E> {

        override fun onStart(requestId: String) {
        }

        override fun onRange(requestId: String, range: IntRange) {
            currentTask
                ?.takeIf { it.requestId == requestId }
                ?.onRange
                ?.invoke(range)
        }

        override fun onInterrupted(requestId: String) {
            currentTask
                ?.takeIf { it.requestId == requestId }
                ?.continuation
                ?.cancel()
            currentTask = null
        }

        override fun onFlushed(requestId: String) {
            currentTask
                ?.takeIf { it.requestId == requestId }
                ?.continuation
                ?.cancel()
            currentTask = null
        }

        override fun onDone(requestId: String) {
            currentTask
                ?.takeIf { it.requestId == requestId }
                ?.continuation
                ?.resume(null) {}
            currentTask = null
        }

        override fun onError(requestId: String, error: E) {
            currentTask
                ?.takeIf { it.requestId == requestId }
                ?.continuation
                ?.resume(error) {}
            currentTask = null
        }
    }
}
