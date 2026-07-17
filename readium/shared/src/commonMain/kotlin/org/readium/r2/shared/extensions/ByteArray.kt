/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.extensions

import org.readium.r2.shared.InternalReadiumApi

internal fun ByteArray.read(range: LongRange?): ByteArray {
    range ?: return this

    @Suppress("NAME_SHADOWING")
    val range = range
        .coerceIn(0L until size)
        .requireLengthFitInt()

    return sliceArray(range.map(Long::toInt))
}
