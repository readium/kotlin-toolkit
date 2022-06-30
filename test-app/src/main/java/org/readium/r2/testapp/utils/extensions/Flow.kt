/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

/**
 * Collects safely the [Flow] as a [State] when the local lifecycle is started.
 */
@Composable
fun <T> Flow<T>.asStateWhenStarted(initialValue: T): State<T> =
    flowWithLocalLifecycle()
        .collectAsState(initial = initialValue)

/**
 * Collects safely the [StateFlow] as a [State] when the local lifecycle is started.
 */
@Composable
fun <T> StateFlow<T>.asStateWhenStarted(): State<T> =
    asStateWhenStarted(transform = { it })

/**
 * Collects safely the [StateFlow] as a [State] when the local lifecycle is started, transforming the
 * value first.
 */
@Composable
fun <T, R> StateFlow<T>.asStateWhenStarted(transform: (T) -> R): State<R> =
    map(transform)
        .flowWithLocalLifecycle()
        .collectAsState(initial = transform(value))

/**
 * Creates a [Flow] emitting values only when the local lifecycle is started.
 *
 * See https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda
 */
@Composable
fun <T> Flow<T>.flowWithLocalLifecycle(minActiveState: Lifecycle.State = Lifecycle.State.STARTED): Flow<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(this, lifecycleOwner) {
        this.flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState)
    }
}

@Composable
fun <T> StateFlow<T>.flowWithLocalLifecycle(minActiveState: Lifecycle.State = Lifecycle.State.STARTED): StateFlow<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(this, lifecycleOwner) {
        this.flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState)
            .stateIn(lifecycleOwner.lifecycleScope, SharingStarted.WhileSubscribed(), initialValue = value)
    }
}

/**
 * Throttles the values of the [Flow] in the given [period].
 *
 * Taken from https://github.com/Kotlin/kotlinx.coroutines/issues/1107#issuecomment-1083076517
 */
fun <T> Flow<T>.throttleLatest(period: Duration): Flow<T> =
    flow {
        conflate().collect {
            emit(it)
            delay(period)
        }
    }
