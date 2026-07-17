/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.logging.ReadiumLog

/**
 * Returns the result of the given [closure], or null if an [Exception] was raised.
 */
@InternalReadiumApi
public inline fun <T> tryOrNull(closure: () -> T): T? =
    tryOr(null, closure)

/**
 * Returns the result of the given [closure], or [default] if an [Exception] was raised.
 */
@InternalReadiumApi
public inline fun <T> tryOr(default: T, closure: () -> T): T =
    try {
        closure()
    } catch (e: Exception) {
        default
    }

/**
 * Returns the result of the given [closure], or null if an [Exception] was raised.
 * The [Exception] will be logged.
 */
@InternalReadiumApi
public inline fun <T> tryOrLog(closure: () -> T): T? =
    try {
        closure()
    } catch (e: Exception) {
        ReadiumLog.e(e)
        null
    }
