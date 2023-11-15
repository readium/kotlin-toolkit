/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError

public sealed class FilesystemError(
    override val message: String,
    override val cause: Error? = null
) : Error {

    public class NotFound(
        cause: Error?
    ) : FilesystemError("File not found.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class Forbidden(
        cause: Error?
    ) : FilesystemError("You are not allowed to access this file.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class Unknown(
        cause: Error?
    ) : FilesystemError("An unexpected error occurred on the filesystem.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }
}
