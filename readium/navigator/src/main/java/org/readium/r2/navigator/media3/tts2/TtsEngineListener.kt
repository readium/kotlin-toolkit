/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.shared.ExperimentalReadiumApi
import timber.log.Timber

@ExperimentalReadiumApi
internal class TtsEngineListener : TtsEngine.Listener {

    private data class UtteranceTask(
        val id: String,
        val continuation: CancellableContinuation<TtsEngine.Exception?>
    )

    data class EngineState(
        val utteranceId: String?,
        val range: IntRange?,
    )

    private val waitingTasks: MutableList<UtteranceTask> =
        mutableListOf()

    private val stateMutable: MutableStateFlow<EngineState> =
        MutableStateFlow(
            EngineState(null, null)
        )

    val state: StateFlow<EngineState> =
        stateMutable.asStateFlow()

    fun addCallback(id: String, continuation: CancellableContinuation<TtsEngine.Exception?>) {
        val task = UtteranceTask(id, continuation)
        waitingTasks.add(task)
    }

    fun removeAllCallbacks() {
        waitingTasks.clear()
    }

    override fun onStart(id: String) {
        Timber.d("onStart")
        stateMutable.value = EngineState(id, null)
    }

    override fun onRange(id: String, range: IntRange) {
        Timber.d("onRange")
        stateMutable.value = EngineState(id, range)
    }

    override fun onInterrupted(id: String) {
        Timber.d("onInterrupted")
        stateMutable.value = EngineState(null, null)
        waitingTasks.forEach {
            it.continuation.resume(null) {}
            waitingTasks.remove(it)
        }
    }

    override fun onFlushed(id: String) {
        Timber.d("onFlushed")
        waitingTasks.forEach {
            it.continuation.resume(null) {}
            waitingTasks.remove(it)
        }
    }

    override fun onDone(id: String) {
        Timber.d("onDone")
        stateMutable.value = EngineState(null, null)
        waitingTasks.forEach {
            it.continuation.resume(null) {}
            waitingTasks.remove(it)
        }
    }

    override fun onError(id: String, error: TtsEngine.Exception) {
        Timber.e("onError $error")
    }
}
