/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.extensions

import kotlin.collections.Iterable

internal fun <T> Iterable<T>.contains(predicate: (T) -> Boolean): Boolean =
    firstOrNull(predicate) != null

internal fun <T> Iterable<T>.containsAny(vararg elements: T): Boolean {
    for (element in elements) {
        if (contains(element)) {
            return true
        }
    }
    return false
}
