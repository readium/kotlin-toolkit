/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.util.File
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.logging.WarningLogger

/**
 *  Parses a Publication from a file.
 */
interface PublicationParser {

    /**
     * Constructs a [Publication.Builder] to build a [Publication] from a publication file.
     *
     * @param file Path to the publication file.
     * @param fetcher Initial leaf fetcher which should be used to read the publication's resources.
     * This can be used to:
     * - support content protection technologies
     * - parse exploded archives or in archiving formats unknown to the parser, e.g. RAR
     * If the file is not an archive, it will be reachable at the HREF /<file.name>,
     * e.g. with a PDF.
     * @param warnings Used to report non-fatal parsing warnings, such as publication authoring
     * mistakes. This is useful to warn users of potential rendering issues or help authors
     * debug their publications.
     */
    suspend fun parse(file: File, fetcher: Fetcher, warnings: WarningLogger? = null): Publication.Builder?

}
