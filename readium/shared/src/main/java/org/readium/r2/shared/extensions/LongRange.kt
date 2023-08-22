/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
public fun LongRange.coerceFirstNonNegative(): LongRange = LongRange(first.coerceAtLeast(0), last)

@InternalReadiumApi
public fun LongRange.coerceIn(range: LongRange): LongRange = LongRange(
    first.coerceAtLeast(range.first),
    last.coerceAtMost(range.last)
)

@InternalReadiumApi
public fun LongRange.requireLengthFitInt(): LongRange = this.apply {
    require(
        last - first + 1 <= Int.MAX_VALUE
    )
}

internal fun LongRange.contains(range: LongRange) =
    contains(range.first) && contains(range.last)
