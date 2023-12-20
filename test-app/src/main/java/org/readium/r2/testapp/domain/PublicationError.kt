/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.asset.AssetOpener
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

sealed class PublicationError(
    override val message: String,
    override val cause: Error? = null
) : Error {

    class ReadError(override val cause: org.readium.r2.shared.util.data.ReadError) :
        PublicationError(cause.message, cause.cause)

    class UnsupportedScheme(cause: Error) :
        PublicationError(cause.message, cause.cause)

    class UnsupportedFormat(cause: Error) :
        PublicationError(cause.message, cause.cause)

    class InvalidPublication(cause: Error) :
        PublicationError(cause.message, cause.cause)

    class Unexpected(cause: Error) :
        PublicationError(cause.message, cause.cause)

    fun toUserError(): UserError =
        when (this) {
            is InvalidPublication ->
                UserError(R.string.publication_error_invalid_publication)
            is Unexpected ->
                UserError(R.string.publication_error_unexpected)
            is UnsupportedFormat ->
                UserError(R.string.publication_error_unsupported_asset)
            is UnsupportedScheme ->
                UserError(R.string.publication_error_scheme_not_supported)
            is ReadError ->
                cause.toUserError()
        }

    companion object {

        operator fun invoke(error: AssetOpener.OpenError): PublicationError =
            when (error) {
                is AssetOpener.OpenError.Reading ->
                    ReadError(error.cause)
                is AssetOpener.OpenError.FormatNotSupported ->
                    UnsupportedFormat(error)
                is AssetOpener.OpenError.SchemeNotSupported ->
                    UnsupportedScheme(error)
            }

        operator fun invoke(error: PublicationOpener.OpenError): PublicationError =
            when (error) {
                is PublicationOpener.OpenError.Reading ->
                    ReadError(error.cause)
                is PublicationOpener.OpenError.FormatNotSupported ->
                    PublicationError(error)
            }
    }
}
