package org.readium.r2.navigator3.core.util

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference

/**
 * Mutual exclusion for UI state mutation over time.
 *
 * [mutate] permits interruptible state mutation over time using a standard [MutatePriority].
 * A [MutatorMutex] enforces that only a single writer can be active at a time for a particular
 * state resource. Instead of queueing callers that would acquire the lock like a traditional
 * [Mutex], new attempts to [mutate] the guarded state will either cancel the current mutator or
 * if the current mutator has a higher priority, the new caller will throw [CancellationException].
 *
 * [MutatorMutex] should be used for implementing hoisted state objects that many mutators may
 * want to manipulate over time such that those mutators can coordinate with one another. The
 * [MutatorMutex] instance should be hidden as an implementation detail. For example:
 *
 * @sample androidx.compose.foundation.samples.mutatorMutexStateObject
 */
@Stable
internal class MutatorMutex<T : Comparable<T>> {

    sealed class LockException : CancellationException() {

        /**
         * Job is the job that cannot be interrupted.
         */
        class CannotInterrupt(val job: Job) : LockException()

        /**
         * Job is the interrupting job.
         */
        sealed class Interrupted(val job: Job) : LockException() {

            class SamePriority(job: Job) : Interrupted(job)

            class HigherPriority(job: Job) : Interrupted(job)
        }
    }

    private class Mutator<T : Comparable<T>>(val priority: T, val job: Job) {
        fun canInterrupt(other: Mutator<T>) = priority >= other.priority

        fun cancel(cause: CancellationException?) = job.cancel(cause)
    }

    private val currentMutator = AtomicReference<Mutator<T>?>(null)
    internal val mutex = Mutex()

    private fun tryMutateOrCancel(mutator: Mutator<T>) {
        while (true) {
            val oldMutator = currentMutator.get()
            if (oldMutator == null || mutator.canInterrupt(oldMutator)) {
                if (currentMutator.compareAndSet(oldMutator, mutator)) {
                    oldMutator?.let {
                        val exception =
                            if (mutator.priority == oldMutator.priority)
                                LockException.Interrupted.SamePriority(mutator.job)
                            else
                                LockException.Interrupted.HigherPriority(mutator.job)
                        it.cancel(exception)
                    }
                    break
                }
            } else throw LockException.CannotInterrupt(oldMutator.job)
        }
    }

    /**
     * Enforce that only a single caller may be active at a time.
     *
     * If [mutate] is called while another call to [mutate] or [mutateWith] is in progress, their
     * [priority] values are compared. If the new caller has a [priority] equal to or higher than
     * the call in progress, the call in progress will be cancelled, throwing
     * [CancellationException] and the new caller's [block] will be invoked. If the call in
     * progress had a higher [priority] than the new caller, the new caller will throw
     * [CancellationException] without invoking [block].
     *
     * @param priority the priority of this mutation; [MutatePriority.Default] by default. Higher
     * priority mutations will interrupt lower priority mutations.
     * @param block mutation code to run mutually exclusive with any other call to [mutate] or
     * [mutateWith].
     */
    suspend fun <R> mutate(
        priority: T,
        block: suspend () -> R
    ) = coroutineScope {
        val mutator = Mutator(priority, coroutineContext[Job]!!)

        tryMutateOrCancel(mutator)

        mutex.withLock {
            try {
                block()
            } finally {
                currentMutator.compareAndSet(mutator, null)
            }
        }
    }

    suspend fun waitAvailability() =
        mutex.withLock {}
}
