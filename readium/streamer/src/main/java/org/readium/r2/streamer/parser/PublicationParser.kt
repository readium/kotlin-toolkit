/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser

import org.readium.r2.shared.error.SimpleError
import org.readium.r2.shared.error.ThrowableError
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType

/**
 *  Parses a Publication from an asset.
 */
interface PublicationParser {

    data class Asset(
        val name: String,
        val mediaType: MediaType,
        val fetcher: Fetcher
    )

    /**
     * Constructs a [Publication.Builder] to build a [Publication] from a publication asset.
     *
     * @param asset Publication asset.
     * @param warnings Used to report non-fatal parsing warnings, such as publication authoring
     * mistakes. This is useful to warn users of potential rendering issues or help authors
     * debug their publications.
     */
    suspend fun parse(
        asset: Asset,
        warnings: WarningLogger? = null
    ): Try<Publication.Builder, Error>

    sealed class Error : org.readium.r2.shared.error.Error {

        class FormatNotSupported : Error() {

            override val message: String =
                "Asset format not supported."

            override val cause: org.readium.r2.shared.error.Error? =
                null
        }

        class ParsingFailed(override val cause: org.readium.r2.shared.error.Error?) : Error() {

            constructor(message: String) : this(SimpleError(message))

            override val message: String =
                "An error occurred while parsing the publication."
        }

        class IO(
            val resourceError: Resource.Exception
        ) : Error() {

            override val message: String =
                "An IO error occurred."

            override val cause: org.readium.r2.shared.error.Error =
                ThrowableError(resourceError)
        }

        class OutOfMemory(
            override val cause: org.readium.r2.shared.error.Error?
        ) : Error() {

            override val message: String =
                "There is not enough memory on the device to parse the publication."
        }
    }
}
