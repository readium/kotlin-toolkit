/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// See https://proandroiddev.com/android-singleliveevent-redux-with-kotlin-flow-b755c70bb055

package org.readium.r2.testapp.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class EventChannel<T>(private val channel: Channel<T>, private val sendScope: CoroutineScope) {

    fun send(event: T) {
        sendScope.launch {
            channel.send(event)
        }
    }

    fun receive(lifecycleOwner: LifecycleOwner, callback: suspend (T) -> Unit) {
        val observer = FlowObserver(lifecycleOwner, channel.receiveAsFlow(), callback)
        lifecycleOwner.lifecycle.addObserver(observer)
    }
}

class FlowObserver<T> (
    private val lifecycleOwner: LifecycleOwner,
    private val flow: Flow<T>,
    private val collector: suspend (T) -> Unit
) : DefaultLifecycleObserver {

    private var job: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        if (job == null) {
            job = lifecycleOwner.lifecycleScope.launch {
                flow.collect { collector(it) }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        job?.cancel()
        job = null
    }
}

inline fun <reified T> Flow<T>.observeWhenStarted(
    lifecycleOwner: LifecycleOwner,
    noinline collector: suspend (T) -> Unit
) {
    val observer = FlowObserver(lifecycleOwner, this, collector)
    lifecycleOwner.lifecycle.addObserver(observer)
}
