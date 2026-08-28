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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Collects safely the [Flow] as a [State] when the local lifecycle is started.
 *
 * See https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda
 */
@Composable
fun <T> Flow<T>.asStateWhenStarted(initialValue: T): State<T> {
    val owner = LocalLifecycleOwner.current
    return remember(this, owner) {
        flowWithLifecycle(owner.lifecycle)
    }.collectAsState(initial = initialValue)
}

/**
 * Collects safely the [StateFlow] as a [State] when the local lifecycle is started.
 */
@Composable
fun <T> StateFlow<T>.asStateWhenStarted(): State<T> =
    asStateWhenStarted(transform = { it })

/**
 * Collects safely the [StateFlow] as a [State] when the local lifecycle is started, transforming the
 * value first.
 *
 * See https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda
 */
@Composable
// This warning is to prevent people from accessing the `value` directly which will not observe
// changes. In this case we're using it for the `initial` value, so it's fine.
@Suppress("StateFlowValueCalledInComposition")
fun <T, R> StateFlow<T>.asStateWhenStarted(transform: (T) -> R): State<R> {
    val owner = LocalLifecycleOwner.current
    return remember(this, owner) {
        map(transform)
            .flowWithLifecycle(owner.lifecycle)
    }.collectAsState(initial = transform(value))
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

suspend fun <P> Flow<P>.stateInFirst(scope: CoroutineScope, sharingStarted: SharingStarted) =
    stateIn(scope, sharingStarted, first())

/**
 * Transforms the value of a [StateFlow] and stores it in a new [StateFlow] using the given
 * [coroutineScope].
 */
fun <T, M> StateFlow<T>.mapStateIn(
    coroutineScope: CoroutineScope,
    transform: (value: T) -> M,
): StateFlow<M> =
    map { transform(it) }
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            transform(value)
        )
