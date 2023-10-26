/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.r2.shared.UserException
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.asset.AssetError
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.testapp.R

sealed class PublicationError(@StringRes userMessageId: Int) : UserException(userMessageId) {

    class Network(val error: Error) : PublicationError(
        R.string.publication_error_network_unexpected
    )

    class Filesystem(val error: Error) : PublicationError(R.string.publication_error_filesystem)

    class NotFound(val error: Error) : PublicationError(R.string.publication_error_not_found)

    class OutOfMemory(val error: Error) : PublicationError(R.string.publication_error_out_of_memory)

    class SchemeNotSupported(val error: Error) : PublicationError(
        R.string.publication_error_scheme_not_supported
    )

    class UnsupportedAsset(val error: Error? = null) : PublicationError(
        R.string.publication_error_unsupported_asset
    )

    class InvalidPublication(val error: Error) : PublicationError(
        R.string.publication_error_invalid_publication
    )

    class IncorrectCredentials(val error: Error) : PublicationError(
        R.string.publication_error_incorrect_credentials
    )

    class Forbidden(val error: Error? = null) : PublicationError(
        R.string.publication_error_forbidden
    )

    class Unexpected(val error: Error) : PublicationError(R.string.publication_error_unexpected)

    companion object {

        operator fun invoke(error: AssetError): PublicationError =
            when (error) {
                is AssetError.Forbidden ->
                    Forbidden(error)
                is AssetError.IncorrectCredentials ->
                    IncorrectCredentials(error)
                is AssetError.NotFound ->
                    NotFound(error)
                is AssetError.OutOfMemory ->
                    OutOfMemory(error)
                is AssetError.InvalidAsset ->
                    InvalidPublication(error)
                is AssetError.Unknown ->
                    Unexpected(error)
                is AssetError.UnsupportedAsset ->
                    UnsupportedAsset(error)
                is AssetError.Filesystem ->
                    Filesystem(error.cause)
                is AssetError.Network ->
                    Network(error.cause)
            }

        operator fun invoke(error: AssetRetriever.Error): PublicationError =
            when (error) {
                is AssetRetriever.Error.ArchiveFormatNotSupported ->
                    UnsupportedAsset(error)
                is AssetRetriever.Error.Forbidden ->
                    Forbidden(error)
                is AssetRetriever.Error.NotFound ->
                    NotFound(error)
                is AssetRetriever.Error.InvalidAsset ->
                    InvalidPublication(error)
                is AssetRetriever.Error.OutOfMemory ->
                    OutOfMemory(error)
                is AssetRetriever.Error.SchemeNotSupported ->
                    SchemeNotSupported(error)
                is AssetRetriever.Error.Unknown ->
                    Unexpected(error)
                is AssetRetriever.Error.Filesystem ->
                    Filesystem(error.cause)
                is AssetRetriever.Error.Network ->
                    Filesystem(error.cause)
            }
    }
}
