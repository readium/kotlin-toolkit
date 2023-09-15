/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.readium.r2.shared.InternalReadiumApi

/**
 * Executes coroutines in a sequential order (FIFO).
 */
@InternalReadiumApi
public class CoroutineQueue(
    public val scope: CoroutineScope = MainScope()
) {
    init {
        scope.launch {
            for (task in tasks) {
                task()
            }
        }
    }

    /**
     * Launches a coroutine in the queue.
     */
    public fun launch(task: suspend () -> Unit) {
        tasks.trySendBlocking(Task(task)).getOrThrow()
    }

    /**
     * Launches a coroutine in the queue, and waits for its result.
     */
    public suspend fun <T> await(task: suspend () -> T): T =
        suspendCancellableCoroutine { cont ->
            tasks.trySendBlocking(Task(task, cont)).getOrThrow()
        }

    /**
     * Cancels all the coroutines in the queue.
     */
    public fun cancel(cause: CancellationException? = null) {
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
