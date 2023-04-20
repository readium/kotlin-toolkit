/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.logging.WarningLogger

/**
 *  Parses a Publication from an asset.
 */
interface PublicationParser {

    /**
     * Constructs a [Publication.Builder] to build a [Publication] from a publication asset.
     *
     * @param asset Publication asset.
     * @param warnings Used to report non-fatal parsing warnings, such as publication authoring
     * mistakes. This is useful to warn users of potential rendering issues or help authors
     * debug their publications.
     */
    suspend fun parse(
        asset: PublicationAsset,
        warnings: WarningLogger? = null
    ): Try<Publication.Builder, Error>


    sealed class Error : Exception() {

        object NotSupported : Error()

        data class ParsingFailed(override val cause: Throwable) : Error()

        data class Unavailable(override val cause: Throwable) : Error()
    }
}
