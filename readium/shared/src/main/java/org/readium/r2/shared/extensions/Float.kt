/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.extensions

import kotlin.math.abs
import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
public fun Float.equalsDelta(other: Float, delta: Float = 0.001f): Boolean =
    this == other || abs(this - other) < delta

@InternalReadiumApi
public fun Double.equalsDelta(other: Double, delta: Double = 0.001): Boolean =
    this == other || abs(this - other) < delta
