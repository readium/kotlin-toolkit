/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

import okio.IOException

/**
 * Signals that a zip exception of some sort has occurred.
 *
 * Common replacement for `java.util.zip.ZipException` in the ported zip stack (phase 05b).
 */
internal open class ZipException(
    message: String? = null,
    cause: Throwable? = null,
) : IOException(message, cause)
