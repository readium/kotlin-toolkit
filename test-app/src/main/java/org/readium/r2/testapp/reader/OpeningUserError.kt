/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import androidx.annotation.StringRes
import org.readium.r2.shared.util.Error
import org.readium.r2.testapp.R
import org.readium.r2.testapp.domain.PublicationUserError
import org.readium.r2.testapp.utils.UserError

sealed class OpeningUserError(
    override val content: UserError.Content,
    override val cause: UserError?
) : UserError {

    constructor(@StringRes userMessageId: Int) :
        this(UserError.Content(userMessageId), null)

    constructor(cause: UserError) :
        this(UserError.Content(cause), cause)

    class PublicationError(
        override val cause: PublicationUserError
    ) : OpeningUserError(cause)

    class RestrictedPublication(val error: Error? = null) :
        OpeningUserError(R.string.publication_error_restricted)

    class AudioEngineInitialization(
        val error: Error
    ) : OpeningUserError(R.string.opening_publication_audio_engine_initialization)

    companion object {

        operator fun invoke(error: OpeningError): OpeningUserError =
            when (error) {
                is OpeningError.AudioEngineInitialization ->
                    AudioEngineInitialization(error)
                is OpeningError.PublicationError ->
                    PublicationError(PublicationUserError(error.cause))
                is OpeningError.RestrictedPublication ->
                    RestrictedPublication(error)
            }
    }
}
