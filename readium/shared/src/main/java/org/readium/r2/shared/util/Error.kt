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
) : Error {

    override val message: String =
        throwable.message ?: "Exception"

    override val cause: Error? =
        null
}

/**
 * A basic [Error] implementation with a message.
 */
public class MessageError(
    override val message: String
) : Error {

    override val cause: Error? = null
}
