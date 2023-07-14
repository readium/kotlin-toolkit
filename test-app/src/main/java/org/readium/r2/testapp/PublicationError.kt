/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import androidx.annotation.StringRes
import org.readium.r2.shared.UserException
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.error.Error
import org.readium.r2.shared.publication.Publication

sealed class PublicationError(@StringRes userMessageId: Int) : UserException(userMessageId) {

    class Unavailable(val error: Error) : PublicationError(R.string.publication_error_unavailable)

    class NotFound(val error: Error) : PublicationError(R.string.publication_error_not_found)

    class OutOfMemory(val error: Error) : PublicationError(R.string.publication_error_out_of_memory)

    class SchemeNotSupported(val error: Error) : PublicationError(R.string.publication_error_scheme_not_supported)

    class UnsupportedPublication(val error: Error? = null) : PublicationError(R.string.publication_error_unsupported_asset)

    class InvalidPublication(val error: Error) : PublicationError(R.string.publication_error_invalid_publication)

    class IncorrectCredentials(val error: Error) : PublicationError(R.string.publication_error_incorrect_credentials)

    class Forbidden(val error: Error? = null) : PublicationError(R.string.publication_error_forbidden)

    class Unexpected(val error: Error) : PublicationError(R.string.publication_error_unexpected)

    companion object {

        operator fun invoke(error: Publication.OpeningException): PublicationError =
            when (error) {
                is Publication.OpeningException.Forbidden ->
                    Forbidden(error)
                is Publication.OpeningException.IncorrectCredentials ->
                    IncorrectCredentials(error)
                is Publication.OpeningException.NotFound ->
                    NotFound(error)
                is Publication.OpeningException.OutOfMemory ->
                    OutOfMemory(error)
                is Publication.OpeningException.ParsingFailed ->
                    InvalidPublication(error)
                is Publication.OpeningException.Unavailable ->
                    Unavailable(error)
                is Publication.OpeningException.Unexpected ->
                    Unexpected(error)
                is Publication.OpeningException.UnsupportedAsset ->
                    SchemeNotSupported(error)
            }

        operator fun invoke(error: AssetRetriever.Error): PublicationError =
            when (error) {
                is AssetRetriever.Error.ArchiveFormatNotSupported ->
                    UnsupportedPublication(error)
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
                is AssetRetriever.Error.Unavailable ->
                    Unavailable(error)
                is AssetRetriever.Error.Unknown ->
                    Unexpected(error)
            }
    }
}
