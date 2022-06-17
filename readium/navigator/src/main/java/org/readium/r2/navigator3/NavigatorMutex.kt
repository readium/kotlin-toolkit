package org.readium.r2.navigator3

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import org.readium.r2.navigator3.core.util.MutatorMutex
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

class NavigatorMutex {

    private enum class Priority {

        RelativeGo,

        RestoreLocation,

        AbsoluteGo,

        RefreshLayout,
    }

    private val mutator: MutatorMutex<Priority> = MutatorMutex()

    private var layoutJob: AtomicReference<Job?> = AtomicReference(null)

    suspend fun<R> refreshLayout(block: suspend () -> R): R {
        Timber.v("refreshLayout")
        return coroutineScope {
            val currentJob = coroutineContext[Job]!!
            layoutJob.set(currentJob)
            try {
                Timber.v("Refresh layout before mutate")
                mutator.mutate(Priority.RefreshLayout) {
                    block().also { Timber.v("Terminating refreshLayout job 1") }
                }
            } finally {
                layoutJob.compareAndSet(currentJob, null)
                Timber.v("Terminating refreshLayout job")
            }
        }
    }

    /**
     * If a relativeGo or absoluteGo block is being executed or interrupts,
     * it will be cancelled and block will be executed.
     * If a refreshLayout block is being executed or interrupts,
     * the execution of block will suspend until the refreshLayout completes and then restart.
     */
    suspend fun<R> absoluteGo(block: suspend () -> R): R {
        Timber.v("absoluteGo")
        layoutJob.get()?.let {
            Timber.v("Joining layout job")
            it.join()
        }
        return try {
            Timber.v("execute block")
            mutator.mutate(Priority.AbsoluteGo, block)
                .also { Timber.d("Absolute go finished") }
        } catch (e: MutatorMutex.LockException.CannotInterrupt) {
            Timber.e(e, "Restarting go.")
            absoluteGo(block)
        } catch (e: MutatorMutex.LockException.Interrupted.HigherPriority) {
            Timber.e(e,"Restarting go. ${mutator.mutex.isLocked}")
            absoluteGo(block)
        } catch (e: MutatorMutex.LockException.Interrupted.SamePriority) {
            Timber.e(e,"Same Priority")
            throw e
        } catch (e: CancellationException) {
            Timber.d("Absolute go cancelled")
            throw e
        }
    }

    /**
     * If a relativeGo is already being executed or interrupts, it will be cancelled and the new one will
     * be started. If any absoluteGo or refreshLayout block is being executed,
     * CancellationException will be immediately thrown.
     * Any new block call will interrupt it.
     */
    suspend fun<R> relativeGo(block: suspend () -> R): R {
        Timber.v("relativeGo")
        return mutator.mutate(Priority.RelativeGo, block)
    }

    suspend fun<R> restoreLocation(block: suspend () -> R): R {
        Timber.v("restoreLocation")
        return mutator.mutate(Priority.RestoreLocation, block)
    }
}