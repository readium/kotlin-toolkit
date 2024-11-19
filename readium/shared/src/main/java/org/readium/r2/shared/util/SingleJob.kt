/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.InternalReadiumApi

/**
 * Runs a single coroutine job at a time.
 *
 * If a previous job is running, cancels it before launching the new one.
 */
@InternalReadiumApi
public class SingleJob(
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private val mutex = Mutex()

    /**
     * Launches a coroutine job.
     *
     * If a previous job is running, cancels it before launching the new one.
     */
    public fun launch(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            mutex.withLock {
                job?.cancelAndJoin()
                job = launch { block() }
            }
        }
    }

    /**
     * Cancels the current job, if any.
     */
    public fun cancel() {
        job?.cancel()
    }
}
