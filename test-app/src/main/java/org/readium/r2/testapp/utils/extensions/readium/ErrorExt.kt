/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions.readium

import android.content.Context
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.testapp.utils.UserError
import org.readium.r2.testapp.utils.getUserMessage
import timber.log.Timber

/**
 * Convenience function to get the description of an error with its cause.
 */
fun Error.toDebugDescription(context: Context): String =
    if (this is ThrowableError<*>) {
        throwable.toDebugDescription(context)
    } else {
        var desc = "${javaClass.nameWithEnclosingClasses()}: $message"
        cause?.let { cause ->
            desc += "\n\n${cause.toDebugDescription(context)}"
        }
        desc
    }

fun Throwable.toDebugDescription(context: Context): String {
    var desc = "${javaClass.nameWithEnclosingClasses()}: "

    desc += (this as? UserError)?.getUserMessage(context)
        ?: localizedMessage ?: message ?: ""
    desc += "\n" + stackTrace.take(2).joinToString("\n").prependIndent("  ")
    cause?.let { cause ->
        desc += "\n\n${cause.toDebugDescription(context)}"
    }
    return desc
}

private fun Class<*>.nameWithEnclosingClasses(): String {
    var name = simpleName
    enclosingClass?.let {
        name = "${it.nameWithEnclosingClasses()}.$name"
    }
    return name
}

// FIXME: to improve
fun Timber.Forest.e(error: Error, message: String? = null) {
    e(Exception(error.message), message)
}

fun Timber.Forest.w(error: Error, message: String? = null) {
    w(Exception(error.message), message)
}

/**
 * Finds the first cause instance of the given type.
 */
inline fun <reified T> Error.asInstance(): T? =
    asInstance(T::class.java)

/**
 * Finds the first cause instance of the given type.
 */
fun <R> Error.asInstance(klass: Class<R>): R? =
    @Suppress("UNCHECKED_CAST")
    when {
        klass.isInstance(this) -> this as R
        else -> cause?.asInstance(klass)
    }
