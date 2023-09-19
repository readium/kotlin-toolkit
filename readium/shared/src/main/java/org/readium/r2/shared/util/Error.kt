/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

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
 * An error caused by the catch of a throwable.
 *
 * @param throwable the cause Throwable
 */
public class ThrowableError(
    public val throwable: Throwable
) : BaseError(throwable.message ?: throwable.toString(), cause = null)

/**
 * A basic [Error] implementation with a message.
 */
public class MessageError(
    override val message: String
) : BaseError(message, cause = null)

/**
 * A basic implementation of [Error] able to print itself in a structured way.
 */
public abstract class BaseError(
    override val message: String,
    override val cause: Error? = null
) : Error {
    override fun toString(): String {
        var desc = "${javaClass.nameWithEnclosingClasses()}: $message"
        if (cause != null) {
            desc += "\n  ${cause.toString().prependIndent("  ")}"
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
}
