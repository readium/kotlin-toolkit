package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.Error
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

    /**
     * The publication can't be opened at the moment, for example because of a networking error.
     * This error is generally temporary, so the operation may be retried or postponed.
     */
    public class Unavailable(cause: Error? = null) :
        AssetError("The publication is not available at the moment.", cause)

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