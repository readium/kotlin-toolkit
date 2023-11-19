/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import org.readium.r2.shared.util.Error

sealed class OpeningError(
    override val cause: Error?
) : Error {

    override val message: String =
        "Could not open publication"

    class PublicationError(
        override val cause: org.readium.r2.testapp.domain.PublicationError
    ) : OpeningError(cause)

    class RestrictedPublication(
        cause: Error
    ) : OpeningError(cause)

    class AudioEngineInitialization(
        cause: Error
    ) : OpeningError(cause)
}
