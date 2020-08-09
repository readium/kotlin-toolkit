/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

fun LongRange.coerceFirstNonNegative() = LongRange(first.coerceAtLeast(0), last)

fun LongRange.coerceIn(range: LongRange) = LongRange(first.coerceAtLeast(range.first), last.coerceAtMost(range.last))

fun LongRange.requireLengthFitInt() = this.apply { require(last - first + 1 <= Int.MAX_VALUE) }
