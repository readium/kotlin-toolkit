/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.file

import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.data.AccessError

/**
 * Errors wrapping file system exceptions.
 */
public sealed class FileSystemError(
    override val message: String,
    override val cause: Error? = null,
) : AccessError {

    public class FileNotFound(
        cause: Error?,
    ) : FileSystemError("File not found.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class Forbidden(
        cause: Error?,
    ) : FileSystemError("You are not allowed to access this file.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class IO(
        cause: Error?,
    ) : FileSystemError("An unexpected IO error occurred on the filesystem.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class InsufficientSpace(
        public val requiredSpace: Long? = null,
        public val freespace: Long? = null,
        cause: Error? = null,
    ) : FileSystemError("There is not enough space to do the operation.", cause)
}
