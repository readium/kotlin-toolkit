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

    companion object {

        operator fun invoke(error: AssetRetriever.Error): PublicationError =
            when (error) {
                is AssetRetriever.Error.Reading ->
                    ReadError(error.cause)
                is AssetRetriever.Error.FormatNotSupported ->
                    UnsupportedArchiveFormat(error)
                is AssetRetriever.Error.SchemeNotSupported ->
                    UnsupportedScheme(error)
            }

        operator fun invoke(error: ContentProtectionSchemeRetriever.Error): PublicationError =
            when (error) {
                is ContentProtectionSchemeRetriever.Error.Reading ->
                    ReadError(error.cause)
                ContentProtectionSchemeRetriever.Error.NotRecognized ->
                    UnsupportedContentProtection(error)
            }

        operator fun invoke(error: PublicationFactory.Error): PublicationError =
            when (error) {
                is PublicationFactory.Error.Reading ->
                    ReadError(error.cause)
                is PublicationFactory.Error.FormatNotSupported ->
                    UnsupportedPublication(error)
                is PublicationFactory.Error.ContentProtectionNotSupported ->
                    UnsupportedContentProtection(error)
            }
    }
}
