/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions.readium

import android.content.Context
import org.readium.r2.shared.UserException
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError

/**
 * Convenience function to get the description of an error with its cause.
 */
fun Error.toDebugDescription(context: Context): String =
    if (this is ThrowableError) {
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

    desc += (this as? UserException)?.getUserMessage(context)
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
