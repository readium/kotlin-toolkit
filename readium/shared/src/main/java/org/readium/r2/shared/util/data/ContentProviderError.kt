/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError

public sealed class ContentProviderError(
    override val message: String,
    override val cause: Error? = null
) : AccessError {

    public class FileNotFound(
        cause: Error?
    ) : ContentProviderError("File not found.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class NotAvailable(
        cause: Error?
    ) : ContentProviderError("Content Provider recently crashed.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class IO(
        override val cause: Error
    ) : ContentProviderError("An IO error occurred.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }
}
