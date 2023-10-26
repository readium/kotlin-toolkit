/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.FilesystemError
import org.readium.r2.shared.util.NetworkError
import org.readium.r2.shared.util.ThrowableError

/**
 * Errors occurring while opening a Publication.
 */
public sealed class AssetError(
    override val message: String,
    override val cause: Error? = null
) : Error {

    /**
     * The file format could not be recognized by any parser.
     */
    public class UnsupportedAsset(
        message: String,
        cause: Error?
    ) : AssetError(message, cause) {
        public constructor(message: String) : this(message, null)
        public constructor(cause: Error? = null) : this("Asset is not supported.", cause)
    }

    /**
     * The publication parsing failed with the given underlying error.
     */
    public class InvalidAsset(
        message: String,
        cause: Error? = null
    ) : AssetError(message, cause) {
        public constructor(cause: Error?) : this(
            "The asset seems corrupted so the publication cannot be opened.",
            cause
        )
    }

    /**
     * The publication file was not found on the file system.
     */
    public class NotFound(cause: Error? = null) :
        AssetError("Asset could not be found.", cause)

    /**
     * We're not allowed to open the publication at all, for example because it expired.
     */
    public class Forbidden(cause: Error? = null) :
        AssetError("You are not allowed to open this publication.", cause)

    public class Network(public override val cause: NetworkError) :
        AssetError("A network error occurred.", cause)

    public class Filesystem(public override val cause: FilesystemError) :
        AssetError("A filesystem error occurred.", cause)

    /**
     * The provided credentials are incorrect and we can't open the publication in a
     * `restricted` state (e.g. for a password-protected ZIP).
     */
    public class IncorrectCredentials(cause: Error? = null) :
        AssetError("Provided credentials were incorrect.", cause)

    /**
     * Opening the publication exceeded the available device memory.
     */
    public class OutOfMemory(cause: Error? = null) :
        AssetError("There is not enough memory available to open the publication.", cause)

    /**
     * An unexpected error occurred.
     */
    public class Unknown(cause: Error? = null) :
        AssetError("An unexpected error occurred.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }
}
