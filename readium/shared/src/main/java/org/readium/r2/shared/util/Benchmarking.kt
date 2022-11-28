/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import timber.log.Timber

@OptIn(ExperimentalTime::class)
internal inline fun <T> benchmark(title: String, enabled: Boolean = true, closure: () -> T): T {
    if (!enabled) {
        return closure()
    }

    var result: T
    val duration = measureTime {
        result = closure()
    }
    Timber.d("""Benchmark "$title" took %.4f seconds """.format(duration.inWholeSeconds))
    return result
}
