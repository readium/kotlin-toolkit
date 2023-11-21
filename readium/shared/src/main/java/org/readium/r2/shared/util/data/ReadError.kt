/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import java.io.IOException
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.ThrowableError

/**
 * Errors occurring while accessing a resource.
 */
public sealed class ReadError(
    override val message: String,
    override val cause: Error? = null
) : Error {

    public class Access(public override val cause: AccessError) :
        ReadError("An error occurred while attempting to access data.", cause)

    public class Decoding(cause: Error? = null) :
        ReadError("An error occurred while attempting to decode the content.", cause) {

        public constructor(message: String) : this(MessageError(message))
        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class OutOfMemory(override val cause: ThrowableError<OutOfMemoryError>) :
        ReadError("The resource is too large to be read on this device.", cause) {

        public constructor(error: OutOfMemoryError) : this(ThrowableError(error))
    }

    public class UnsupportedOperation(cause: Error? = null) :
        ReadError("Could not proceed because an operation was not supported.", cause) {

        public constructor(message: String) : this(MessageError(message))
    }
}

public interface AccessError : Error

public class ReadException(
    public val error: ReadError
) : IOException(error.message, ErrorException(error))
