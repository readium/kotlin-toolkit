/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.readium.r2.shared.InternalReadiumApi
import timber.log.Timber

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
 * A basic [Error] implementation with a message.
 */
public class MessageError(
    override val message: String,
    override val cause: Error? = null
) : Error

/**
 * An error caused by the catch of a throwable.
 */
public class ThrowableError<E : Throwable>(
    public val throwable: E
) : Error {
    override val message: String = throwable.message ?: throwable.toString()
    override val cause: Error? = throwable.cause?.let { ThrowableError(it) }
}

/**
 * A throwable caused by an [Error].
 */
public class ErrorException(
    public val error: Error
) : Exception(error.message, error.cause?.let { ErrorException(it) })

public fun <S, F> Try<S, F>.assertSuccess(): S =
    when (this) {
        is Try.Success ->
            value
        is Try.Failure ->
            throw IllegalStateException(
                "Try was excepted to contain a success.",
                value as? Throwable
            )
    }

public class FilesystemError(
    override val cause: Error? = null
) : Error {

    public constructor(exception: Exception) : this(ThrowableError(exception))

    override val message: String =
        "An unexpected error occurred on the filesystem."
}

// FIXME: to improve
@InternalReadiumApi
public fun Timber.Forest.e(error: Error, message: String? = null) {
    e(Exception(error.message), message)
}

@InternalReadiumApi
public fun Timber.Forest.w(error: Error, message: String? = null) {
    w(Exception(error.message), message)
}
