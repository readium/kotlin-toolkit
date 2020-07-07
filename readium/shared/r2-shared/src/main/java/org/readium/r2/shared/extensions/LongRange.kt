/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

internal fun LongRange.coerceToPositiveIncreasing() =
    if (first >= last)
        0L until 0L
    else
        LongRange(first.coerceAtLeast(0), last.coerceAtLeast(0))

internal fun LongRange.requireLengthFitInt() = require(last - first + 1 <= Int.MAX_VALUE)
