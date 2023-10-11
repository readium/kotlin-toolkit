/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Executes coroutines in a sequential order (FIFO).
 */
class CoroutineQueue(
    val scope: CoroutineScope = MainScope()
) {
    init {
        scope.launch {
            for (task in tasks) {
                scope.launch {
                    try {
                        task()
                    } catch (e: Exception) {
                        // Exceptions are propagated only when run with await.
                        task.continuation?.resumeWithException(e)
                    }
                }
            }
        }
    }

    /**
     * Launches a coroutine in the queue.
     */
    fun launch(task: suspend () -> Unit) {
        tasks.trySendBlocking(Task(task)).getOrThrow()
    }

    /**
     * Launches a coroutine in the queue, and waits for its result.
     */
    suspend fun <T> await(task: suspend () -> T): T =
        suspendCancellableCoroutine { cont ->
            tasks.trySendBlocking(Task(task, cont)).getOrThrow()
        }

    /**
     * Cancels all the coroutines in the queue.
     *
     * This [CoroutineQueue] can no longer be used.
     */
    fun cancel(cause: CancellationException? = null) {
        scope.cancel(cause)
    }

    private val tasks: Channel<Task<*>> = Channel(Channel.UNLIMITED)

    private class Task<T>(
        val task: suspend () -> T,
        val continuation: CancellableContinuation<T>? = null
    ) {
        suspend operator fun invoke() {
            val result = task()
            continuation?.resume(result)
        }
    }
}
