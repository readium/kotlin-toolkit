/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.logging.WarningLogger

/**
 *  Parses a Publication from an asset.
 */
public interface PublicationParser {

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

    public sealed class Error(
        public override val message: String,
        public override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public class FormatNotSupported :
            Error("Asset format not supported.", null)

        public class Reading(override val cause: org.readium.r2.shared.util.data.ReadError) :
            Error("An error occurred while trying to read asset.", cause)
    }
}
