/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

public sealed class NetworkError(
    public override val message: String,
    public override val cause: Error? = null
) : Error {

    /** Equivalent to a 400 HTTP error. */
    public class BadRequest(cause: Error? = null) :
        NetworkError("Invalid request which can't be processed", cause) {

        public constructor(message: String) : this(MessageError(message))

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    /**
     * Equivalent to a 503 HTTP error.
     *
     * Used when the source can't be reached, e.g. no Internet connection, or an issue with the
     * file system. Usually this is a temporary error.
     */
    public class Unavailable(cause: Error? = null) :
        NetworkError("The resource is currently unavailable, please try again later.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    /**
     * The Internet connection appears to be offline.
     */
    public class Offline(cause: Error? = null) :
        NetworkError("The Internet connection appears to be offline.", cause)
}
