/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.extensions

/**
 * Splits a [String] in two components, at the given delimiter.
 */
// FIXME: Move to an internal module when the monorepo will be setup
internal fun String.splitAt(delimiter: String): Pair<String, String?> {
    val components = split(delimiter, limit = 2)
    return Pair(components[0], components.getOrNull(1))
}
