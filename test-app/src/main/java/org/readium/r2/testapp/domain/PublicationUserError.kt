/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.r2.shared.util.Error
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

sealed class PublicationUserError(
    override val content: UserError.Content,
    override val cause: UserError? = null
) : UserError {

    constructor(@StringRes userMessageId: Int) :
        this(UserError.Content(userMessageId), null)

    class ReadError(override val cause: ReadUserError) :
        PublicationUserError(cause.content, cause.cause)

    class UnsupportedScheme(val error: Error) :
        PublicationUserError(R.string.publication_error_scheme_not_supported)

    class UnsupportedContentProtection(val error: Error? = null) :
        PublicationUserError(R.string.publication_error_unsupported_protection)
    class UnsupportedArchiveFormat(val error: Error) :
        PublicationUserError(R.string.publication_error_unsupported_archive)

    class UnsupportedPublication(val error: Error? = null) :
        PublicationUserError(R.string.publication_error_unsupported_asset)

    class InvalidPublication(val error: Error) :
        PublicationUserError(R.string.publication_error_invalid_publication)

    class Unexpected(val error: Error) :
        PublicationUserError(R.string.publication_error_unexpected)

    companion object {

        operator fun invoke(error: PublicationError): PublicationUserError =
            when (error) {
                is PublicationError.InvalidPublication ->
                    InvalidPublication(error)

                is PublicationError.Unexpected ->
                    Unexpected(error)

                is PublicationError.UnsupportedArchiveFormat ->
                    UnsupportedArchiveFormat(error)

                is PublicationError.UnsupportedContentProtection ->
                    UnsupportedContentProtection(error)

                is PublicationError.UnsupportedPublication ->
                    UnsupportedPublication(error)

                is PublicationError.UnsupportedScheme ->
                    UnsupportedScheme(error)

                is PublicationError.ReadError ->
                    ReadError(ReadUserError(error.cause))
            }
    }
}
