/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.r2.shared.UserException
import org.readium.r2.shared.util.Error
import org.readium.r2.testapp.R

sealed class OpeningError(
    content: Content,
    cause: Exception?
) : UserException(content, cause) {

    constructor(@StringRes userMessageId: Int) :
        this(Content(userMessageId), null)

    constructor(cause: UserException) :
        this(Content(cause), cause)

    class PublicationError(
        override val cause: org.readium.r2.testapp.domain.PublicationError
    ) : OpeningError(cause)

    class AudioEngineInitialization(
        val error: Error
    ) : OpeningError(R.string.opening_publication_audio_engine_initialization)
}
