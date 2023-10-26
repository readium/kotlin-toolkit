/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.FilesystemError
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.NetworkError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.ResourceError

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

    public sealed class Error(public override val message: String) :
        org.readium.r2.shared.util.Error {

        public class UnsupportedFormat :
            Error("Asset format not supported.") {

            override val cause: org.readium.r2.shared.util.Error? =
                null
        }

        public class InvalidAsset(override val cause: org.readium.r2.shared.util.Error?) :
            Error("An error occurred while parsing the publication.") {

            public constructor(message: String) : this(MessageError(message))
        }

        public class Forbidden(public override val cause: org.readium.r2.shared.util.Error?) :
            Error("Access to some content was forbidden.")

        public class Network(public override val cause: NetworkError) :
            Error("A network error occurred.")

        public class Filesystem(public override val cause: FilesystemError) :
            Error("A filesystem error occurred.")

        public class OutOfMemory(override val cause: ThrowableError<OutOfMemoryError>) :
            Error("The resource is too large to be read on this device.") {

            public constructor(error: OutOfMemoryError) : this(ThrowableError(error))
        }

        /** For any other error, such as HTTP 500. */
        public class Other(public override val cause: org.readium.r2.shared.util.Error) :
            Error("A service error occurred") {

            public constructor(exception: Exception) : this(ThrowableError(exception))
        }

        internal companion object {

            fun ResourceError.toParserError() =
                when (this) {
                    is ResourceError.Filesystem ->
                        Filesystem(cause)
                    is ResourceError.Forbidden ->
                        Forbidden(cause)
                    is ResourceError.InvalidContent ->
                        InvalidAsset(this)
                    is ResourceError.Network ->
                        Network(cause)
                    is ResourceError.NotFound ->
                        InvalidAsset(this)
                    is ResourceError.Other ->
                        Other(this)
                    is ResourceError.OutOfMemory ->
                        OutOfMemory(cause)
                }
        }
    }
}
