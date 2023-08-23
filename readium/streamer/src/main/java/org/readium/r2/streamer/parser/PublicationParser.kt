/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType

/**
 *  Parses a Publication from an asset.
 */
public interface PublicationParser {

    /**
     * Full publication asset.
     *
     * @param mediaType Media type of the "virtual" publication asset, built from the source asset.
     * For example, if the source asset was a `application/audiobook+json`, the "virtual" asset
     * media type will be `application/audiobook+zip`.
     * @param container Container granting access to the resources of the publication.
     */
    public data class Asset(
        val mediaType: MediaType,
        val container: Container
    )

    /**
     * Constructs a [Publication.Builder] to build a [Publication] from a publication asset.
     *
     * @param asset Publication asset.
     * @param warnings Used to report non-fatal parsing warnings, such as publication authoring
     * mistakes. This is useful to warn users of potential rendering issues or help authors
     * debug their publications.
     */
    public suspend fun parse(
        asset: Asset,
        warnings: WarningLogger? = null
    ): Try<Publication.Builder, Error>

    public sealed class Error : org.readium.r2.shared.util.Error {

        public class FormatNotSupported : Error() {

            override val message: String =
                "Asset format not supported."

            override val cause: org.readium.r2.shared.util.Error? =
                null
        }

        public class ParsingFailed(override val cause: org.readium.r2.shared.util.Error?) : Error() {

            public constructor(message: String) : this(MessageError(message))

            override val message: String =
                "An error occurred while parsing the publication."
        }

        public class IO(
            public val resourceError: Resource.Exception
        ) : Error() {

            override val message: String =
                "An IO error occurred."

            override val cause: org.readium.r2.shared.util.Error =
                ThrowableError(resourceError)
        }

        public class OutOfMemory(
            override val cause: org.readium.r2.shared.util.Error?
        ) : Error() {

            override val message: String =
                "There is not enough memory on the device to parse the publication."
        }
    }
}
