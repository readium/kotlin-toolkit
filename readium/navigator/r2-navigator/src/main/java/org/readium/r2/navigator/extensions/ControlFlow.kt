/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.extensions

/**
 * Unwraps the two given arguments and pass them to the [closure] if they are not null.
 */
internal inline fun <A, B, R> let(a: A?, b: B?, closure: (A, B) -> R?): R? =
    if (a == null || b == null) {
        null
    } else {
        closure(a, b)
    }
