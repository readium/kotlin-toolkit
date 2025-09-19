/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.reflect.KProperty

public data class MutableRef<T>(var value: T)

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> MutableRef<T>.getValue(
    thisObj: Any?,
    property: KProperty<*>,
): T = value

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> MutableRef<T>.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: T,
) {
    this.value = value
}

/**
 * Unlike rememberUpdatedState, this doesn't trigger recomposition.
 */
@Composable
public fun <T> rememberUpdatedRef(value: T): MutableRef<T> = remember {
    MutableRef(value)
}.apply { this.value = value }
