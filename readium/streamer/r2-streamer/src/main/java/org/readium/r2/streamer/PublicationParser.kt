/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.util.File
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.streamer.extensions.toTitle

/**
 *  Parses a Publication from a file.
 */
interface PublicationParser {

    /**
     * Constructs a PublicationBuilder to build a Publication from a publication file.
     *
     * @param file Path to the publication file.
     * @param fetcher Initial leaf fetcher which should be used to read the publication's resources.
     * This can be used to:
     * - support content protection technologies
     * - parse exploded archives or in archiving formats unknown to the parser, e.g. RAR
     * If the file is not an archive, it will be reachable at the HREF /publication.<file.format.fileExtension>,
     * e.g. with a PDF.
     * @param fallbackTitle Publication's title is mandatory,
     * but some formats might not have a way of declaring a title.
     * In which case, fallbackTitle will be used.
     * @param warnings used to broadcast non-fatal parsing warnings.
     * Can be used to report publication authoring mistakes,
     * to warn users of potential rendering issues or help authors debug their publications.
     */
    suspend fun parse(file: File, fetcher: Fetcher, fallbackTitle: String, warnings: WarningLogger? = null)
            : Try<PublicationBuilder, Throwable>?

    /**
     * Builds a Publication from its components.
     *
     * A Publication's construction is distributed over the Streamer and its parsers,
     * so a builder is useful to pass the parts around.
     */
    data class PublicationBuilder(
        var manifest: Manifest,
        var fetcher: Fetcher,
        var servicesBuilder: Publication.ServicesBuilder
    ) {

        fun build(): Publication = Publication(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = servicesBuilder
        )
    }
}
