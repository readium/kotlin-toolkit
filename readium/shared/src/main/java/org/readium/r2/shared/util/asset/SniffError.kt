/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.data.ReadError

public sealed class SniffError(
    override val message: String,
    override val cause: Error?
) : Error {

    public data object NotRecognized :
        SniffError("Format of resource could not be inferred.", null)

    public data class Reading(override val cause: ReadError) :
        SniffError("An error occurred while trying to read content.", cause)
}
