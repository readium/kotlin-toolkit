/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.readium.r2.shared.InternalReadiumApi

/**
 * Transforms the value of a [StateFlow] and stores it in a new [StateFlow] using the given
 * [coroutineScope].
 */
@InternalReadiumApi
fun <T, M> StateFlow<T>.mapStateIn(
    coroutineScope: CoroutineScope,
    transform: (value: T) -> M
): StateFlow<M> =
    map { transform(it) }
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            transform(value)
        )
