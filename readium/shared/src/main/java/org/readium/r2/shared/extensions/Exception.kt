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
import timber.log.Timber

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
        Timber.e(e)
        null
    }

/**
 * Finds the first cause instance of the given type.
 */
@InternalReadiumApi
public inline fun <reified T : Throwable> Throwable.findInstance(): T? =
    findInstance(T::class.java)

/**
 * Finds the first cause instance of the given type.
 */
@InternalReadiumApi
public fun <R : Throwable> Throwable.findInstance(klass: Class<R>): R? =
    @Suppress("UNCHECKED_CAST")
    when {
        klass.isInstance(this) -> this as R
        else -> cause?.findInstance(klass)
    }
