/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.content

import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.data.AccessError

/**
 * Errors wrapping Android ContentResolver errors.
 */
public sealed class ContentResolverError(
    override val message: String,
    override val cause: Error? = null,
) : AccessError {

    public class FileNotFound(
        cause: Error? = null,
    ) : ContentResolverError("File not found.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class Forbidden(
        cause: Error?,
    ) : ContentResolverError("You are not allowed to access this file.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class NotAvailable(
        cause: Error? = null,
    ) : ContentResolverError("Content Provider recently crashed.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class IO(
        override val cause: Error,
    ) : ContentResolverError("An IO error occurred.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }
}
