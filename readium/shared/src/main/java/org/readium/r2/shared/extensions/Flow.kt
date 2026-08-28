/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.readium.r2.shared.InternalReadiumApi

/**
 * Transforms the value of a [StateFlow] and stores it in a new [StateFlow] using the given
 * [coroutineScope].
 */
@InternalReadiumApi
public fun <T, M> StateFlow<T>.mapStateIn(
    coroutineScope: CoroutineScope,
    transform: (value: T) -> M,
): StateFlow<M> =
    map { transform(it) }
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            transform(value)
        )

/**
 * Transforms the values of two [StateFlow]s and stores the result in a new [StateFlow] using the
 * given [coroutineScope].
 */
@InternalReadiumApi
public fun <T1, T2, R> StateFlow<T1>.combineStateIn(
    coroutineScope: CoroutineScope,
    flow: StateFlow<T2>,
    transform: (a: T1, b: T2) -> R,
): StateFlow<R> =
    this.combine(flow, transform)
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            transform(value, flow.value)
        )
