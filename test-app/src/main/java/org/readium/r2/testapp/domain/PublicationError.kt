/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.protection.ContentProtectionSchemeRetriever
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.data.ContentProviderError
import org.readium.r2.shared.util.data.FileSystemError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.http.HttpError
import org.readium.r2.shared.util.http.HttpStatus
import org.readium.r2.streamer.PublicationFactory
import org.readium.r2.testapp.R

sealed class PublicationError(@StringRes userMessageId: Int) : UserException(userMessageId) {

    class HttpNotFound(val error: Error) :
        PublicationError(R.string.publication_error_network_not_found)

    class HttpForbidden(val error: Error) :
        PublicationError(R.string.publication_error_network_forbidden)

    class HttpConnectivity(val error: Error) :
        PublicationError(R.string.publication_error_network_connectivity)

    class HttpUnexpected(val error: Error) :
        PublicationError(R.string.publication_error_network_unexpected)

    class FsNotFound(val error: Error) :
        PublicationError(R.string.publication_error_filesystem_not_found)

    class FsUnexpected(val error: Error) :
        PublicationError(R.string.publication_error_filesystem_unexpected)

    class OutOfMemory(val error: Error) :
        PublicationError(R.string.publication_error_out_of_memory)

    class UnsupportedScheme(val error: Error) :
        PublicationError(R.string.publication_error_scheme_not_supported)

    class UnsupportedContentProtection(val error: Error? = null) :
        PublicationError(R.string.publication_error_unsupported_protection)
    class UnsupportedArchiveFormat(val error: Error) :
        PublicationError(R.string.publication_error_unsupported_archive)

    class UnsupportedPublication(val error: Error? = null) :
        PublicationError(R.string.publication_error_unsupported_asset)

    class InvalidPublication(val error: Error) :
        PublicationError(R.string.publication_error_invalid_publication)

    class RestrictedPublication(val error: Error? = null) :
        PublicationError(R.string.publication_error_restricted)

    class Unexpected(val error: Error) :
        PublicationError(R.string.publication_error_unexpected)

    companion object {

        operator fun invoke(error: AssetRetriever.Error): PublicationError =
            when (error) {
                is AssetRetriever.Error.AccessError ->
                    PublicationError(error.cause)
                is AssetRetriever.Error.ArchiveFormatNotSupported ->
                    UnsupportedArchiveFormat(error)
                is AssetRetriever.Error.SchemeNotSupported ->
                    UnsupportedScheme(error)
            }

        operator fun invoke(error: ContentProtectionSchemeRetriever.Error): PublicationError =
            when (error) {
                is ContentProtectionSchemeRetriever.Error.AccessError ->
                    PublicationError(error.cause)
                ContentProtectionSchemeRetriever.Error.NoContentProtectionFound ->
                    UnsupportedContentProtection()
            }

        operator fun invoke(error: PublicationFactory.Error): PublicationError =
            when (error) {
                is PublicationFactory.Error.ReadError ->
                    PublicationError(error.cause)
                is PublicationFactory.Error.UnsupportedAsset ->
                    UnsupportedPublication(error)
                is PublicationFactory.Error.UnsupportedContentProtection ->
                    UnsupportedContentProtection(error)
            }

        operator fun invoke(error: ReadError): PublicationError =
            when (error) {
                is ReadError.Access ->
                    when (val cause = error.cause) {
                        is HttpError -> PublicationError(cause)
                        is FileSystemError -> PublicationError(cause)
                        is ContentProviderError -> PublicationError(cause)
                        else -> Unexpected(cause)
                    }
                is ReadError.Decoding -> InvalidPublication(error)
                is ReadError.Other -> Unexpected(error)
                is ReadError.OutOfMemory -> OutOfMemory(error)
                is ReadError.UnsupportedOperation -> Unexpected(error)
            }

        private operator fun invoke(error: HttpError): PublicationError =
            when (error) {
                is HttpError.IO ->
                    HttpUnexpected(error)
                is HttpError.MalformedResponse ->
                    HttpUnexpected(error)
                is HttpError.Redirection ->
                    HttpUnexpected(error)
                is HttpError.Timeout ->
                    HttpConnectivity(error)
                is HttpError.UnreachableHost ->
                    HttpConnectivity(error)
                is HttpError.Response ->
                    when (error.status) {
                        HttpStatus.Forbidden -> HttpForbidden(error)
                        HttpStatus.NotFound -> HttpNotFound(error)
                        else -> HttpUnexpected(error)
                    }
            }

        private operator fun invoke(error: FileSystemError): PublicationError =
            when (error) {
                is FileSystemError.Forbidden -> FsUnexpected(error)
                is FileSystemError.IO -> FsUnexpected(error)
                is FileSystemError.NotFound -> FsNotFound(error)
            }

        private operator fun invoke(error: ContentProviderError): PublicationError =
            when (error) {
                is ContentProviderError.FileNotFound -> FsNotFound(error)
                is ContentProviderError.IO -> FsUnexpected(error)
                is ContentProviderError.NotAvailable -> FsUnexpected(error)
            }
    }
}
