/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import timber.log.Timber

/**
 * Returns the result of the given [closure], or null if an [Exception] was raised.
 */
inline fun <T> tryOrNull(closure: () -> T): T? =
    tryOr(null, closure)

/**
 * Returns the result of the given [closure], or [default] if an [Exception] was raised.
 */
inline fun <T> tryOr(default: T, closure: () -> T): T =
    try { closure() } catch (e: Exception) { default }

/**
 * Returns the result of the given [closure], or null if an [Exception] was raised.
 * The [Exception] will be logged.
 */
inline fun <T> tryOrLog(closure: () -> T): T? =
    try { closure() } catch (e: Exception) {
        Timber.e(e)
        null
    }

/**
 * Finds the first cause instance of the given type.
 */
inline fun <reified T> Throwable.asInstance(): T? =
    asInstance(T::class.java)

/**
 * Finds the first cause instance of the given type.
 */
fun <R> Throwable.asInstance(klass: Class<R>): R? =
    @Suppress("UNCHECKED_CAST")
    when {
        klass.isInstance(this) -> this as R
        else -> cause?.asInstance(klass)
    }
