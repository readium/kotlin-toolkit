/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.readium.r2.shared.InternalReadiumApi

/**
 * CoroutineScope-like util to execute coroutines in a sequential order (FIFO).
 * As with a SupervisorJob, children can be cancelled or fail independently one from the other.
 */
@InternalReadiumApi
public class CoroutineQueue(
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val scope: CoroutineScope =
        CoroutineScope(dispatcher + SupervisorJob())

    private val tasks: Channel<Task<*>> = Channel(Channel.UNLIMITED)

    init {
        scope.launch {
            for (task in tasks) {
                // Don't fail the root job if one task fails.
                supervisorScope {
                    task()
                }
            }
        }
    }

    /**
     * Launches a coroutine in the queue.
     *
     * Exceptions thrown by [block] will be ignored.
     */
    public fun launch(block: suspend () -> Unit) {
        tasks.trySendBlocking(Task(block)).getOrThrow()
    }

    /**
     * Creates a coroutine in the queue and returns its future result
     * as an implementation of Deferred.
     *
     * Exceptions thrown by [block] will be caught and represented in the resulting [Deferred].
     */
    public fun <T> async(block: suspend () -> T): Deferred<T> {
        val deferred = CompletableDeferred<T>()
        val task = Task(block, deferred)
        tasks.trySendBlocking(task).getOrThrow()
        return deferred
    }

    /**
     * Launches a coroutine in the queue, and waits for its result.
     *
     * Exceptions thrown by [block] will be rethrown.
     */
    public suspend fun <T> await(block: suspend () -> T): T =
        async(block).await()

    /**
     * Cancels this coroutine queue, including all its children with an optional cancellation cause.
     */
    public fun cancel(cause: CancellationException? = null) {
        scope.cancel(cause)
    }

    private class Task<T>(
        val task: suspend () -> T,
        val deferred: CompletableDeferred<T>? = null,
    ) {
        suspend operator fun invoke() {
            try {
                val result = task()
                deferred?.complete(result)
            } catch (e: Exception) {
                deferred?.completeExceptionally(e)
            }
        }
    }
}
