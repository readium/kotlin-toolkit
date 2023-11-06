/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import java.io.IOException
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.FilesystemError
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.ThrowableError

/**
 * Errors occurring while accessing a resource.
 */
public sealed class ReadError(
    override val message: String,
    override val cause: Error? = null
) : Error {

    public class Network(public override val cause: Error) :
        ReadError("A network error occurred.", cause)

    public class Filesystem(public override val cause: FilesystemError) :
        ReadError("A filesystem error occurred.", cause)

    /**
     * Equivalent to a 507 HTTP error.
     *
     * Used when the requested range is too large to be read in memory.
     */
    public class OutOfMemory(override val cause: ThrowableError<OutOfMemoryError>) :
        ReadError("The resource is too large to be read on this device.", cause) {

        public constructor(error: OutOfMemoryError) : this(ThrowableError(error))
    }

    public class Content(cause: Error? = null) :
        ReadError("Content seems invalid. ", cause) {

        public constructor(message: String) : this(MessageError(message))
        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    /** For any other error, such as HTTP 500. */
    public class Other(cause: Error) :
        ReadError("An unclassified error occurred.", cause) {

        public constructor(message: String) : this(MessageError(message))
        public constructor(exception: Exception) : this(ThrowableError(exception))
    }
}

public class AccessException(
    public val error: ReadError
) : IOException(error.message, ErrorException(error))

internal fun Exception.unwrapAccessException(): Exception {
    fun Throwable.findResourceExceptionCause(): AccessException? =
        when {
            this is AccessException -> this
            cause != null -> cause!!.findResourceExceptionCause()
            else -> null
        }

    this.findResourceExceptionCause()?.let { return it }
    return this
}
