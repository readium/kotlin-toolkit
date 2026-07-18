/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// TODO(kmp): move to commonMain — blocked by: JVM String.format
@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util

import kotlin.time.measureTime
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.logging.ReadiumLog

internal inline fun <T> benchmark(title: String, enabled: Boolean = true, closure: () -> T): T {
    if (!enabled) {
        return closure()
    }

    var result: T
    val duration = measureTime {
        result = closure()
    }
    ReadiumLog.d("""Benchmark "$title" took %.4f seconds """.format(duration.inWholeSeconds))
    return result
}
