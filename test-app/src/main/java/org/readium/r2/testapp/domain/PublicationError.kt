/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import org.readium.r2.shared.publication.protection.ContentProtectionSchemeRetriever
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationFactory
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

    class UnsupportedContentProtection(cause: Error) :
        PublicationError(cause.message, cause.cause)
    class UnsupportedArchiveFormat(cause: Error) :
        PublicationError(cause.message, cause.cause)

    class UnsupportedPublication(cause: Error) :
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
            is UnsupportedArchiveFormat ->
                UserError(R.string.publication_error_unsupported_archive)
            is UnsupportedContentProtection ->
                UserError(R.string.publication_error_unsupported_protection)
            is UnsupportedPublication ->
                UserError(R.string.publication_error_unsupported_asset)
            is UnsupportedScheme ->
                UserError(R.string.publication_error_scheme_not_supported)
            is ReadError ->
                cause.toUserError()
        }

    companion object {

        operator fun invoke(error: AssetRetriever.RetrieveError): PublicationError =
            when (error) {
                is AssetRetriever.RetrieveError.Reading ->
                    ReadError(error.cause)
                is AssetRetriever.RetrieveError.FormatNotSupported ->
                    UnsupportedArchiveFormat(error)
                is AssetRetriever.RetrieveError.SchemeNotSupported ->
                    UnsupportedScheme(error)
            }

        operator fun invoke(error: ContentProtectionSchemeRetriever.Error): PublicationError =
            when (error) {
                is ContentProtectionSchemeRetriever.Error.Reading ->
                    ReadError(error.cause)
                ContentProtectionSchemeRetriever.Error.NotRecognized ->
                    UnsupportedContentProtection(error)
            }

        operator fun invoke(error: PublicationFactory.OpenError): PublicationError =
            when (error) {
                is PublicationFactory.OpenError.Reading ->
                    ReadError(error.cause)
                is PublicationFactory.OpenError.FormatNotSupported ->
                    UnsupportedPublication(error)
                is PublicationFactory.OpenError.ContentProtectionNotSupported ->
                    UnsupportedContentProtection(error)
            }
    }
}
