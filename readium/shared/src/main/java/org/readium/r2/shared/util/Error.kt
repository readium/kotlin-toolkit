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
 * A basic [Error] implementation with a message.
 */
public class MessageError(
    override val message: String,
    override val cause: Error? = null
) : Error

/**
 * An error caused by the catch of a throwable.
 */
public class ThrowableError(
    public val throwable: Throwable
) : Error {
    override val message: String = throwable.message ?: throwable.toString()
    override val cause: Error? = null
}

/**
 * A throwable caused by an [Error].
 */
public class ErrorException(
    public val error: Error
) : Exception(error.message)
