package org.readium.r2.testapp.utils

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
    try {
        closure()
    } catch (e: Exception) {
        default
    }

/**
 * Returns the result of the given [closure], or null if an [Exception] was raised.
 * The [Exception] will be logged.
 */
inline fun <T> tryOrLog(closure: () -> T): T? =
    try {
        closure()
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
