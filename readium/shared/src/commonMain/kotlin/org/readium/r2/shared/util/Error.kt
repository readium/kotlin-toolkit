/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.reflect.KClass

/**
 * Describes an error.
 */
public interface Error {

    /**
     * An error message.
     */
    public val message: String

    /**
     * The cause error or null if there is none.
     */
    public val cause: Error?
}

/**
 * A basic [Error] implementation with a debug message.
 */
public class DebugError(
    override val message: String,
    override val cause: Error? = null,
) : Error

/**
 * An error caused by the catch of a throwable.
 */
public class ThrowableError<E : Throwable>(
    public val throwable: E,
) : Error {
    override val message: String = throwable.message ?: throwable.toString()
    override val cause: Error? = throwable.cause?.let { ThrowableError(it) }
}

/**
 * A throwable caused by an [Error].
 */
public class ErrorException(
    public val error: Error,
) : Exception(error.message, error.cause?.let { ErrorException(it) })

/**
 * Convenience function to get the description of an error with its cause.
 */
public fun Error.toDebugDescription(): String =
    if (this is ThrowableError<*>) {
        throwable.toDebugDescription()
    } else {
        var desc = "${this::class.nameWithEnclosingClasses()}: $message"
        cause?.let { cause ->
            desc += "\n${cause.toDebugDescription()}"
        }
        desc
    }

private fun Throwable.toDebugDescription(): String {
    var desc = "${this::class.nameWithEnclosingClasses()}: "

    desc += message ?: ""
    // Formats the first two stack frames like the JVM `StackTraceElement.toString()` used to,
    // e.g. `  org.readium.r2.shared.Foo.bar(Foo.kt:12)`.
    desc += "\n" + stackTraceToString()
        .lines().drop(1).take(2)
        .joinToString("\n") { "  " + it.trim().removePrefix("at ") }
    cause?.let { cause ->
        desc += "\n${cause.toDebugDescription()}"
    }
    return desc
}

private fun KClass<*>.nameWithEnclosingClasses(): String =
    qualifiedName
        // Drops the package segments (by convention, the ones starting with a lowercase letter)
        // to keep only the class name with its enclosing classes, e.g. `Locator.Locations`.
        ?.split(".")
        ?.dropWhile { it.firstOrNull()?.isLowerCase() == true }
        ?.joinToString(".")
        ?.takeUnless { it.isEmpty() }
        ?: simpleName
        ?: "Unknown"
