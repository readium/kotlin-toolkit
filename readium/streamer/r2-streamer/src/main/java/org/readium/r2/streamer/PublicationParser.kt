/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.util.logging.WarningLogger

/**
 *  Parses a Publication from an asset.
 */
interface PublicationParser {

    /**
     * Constructs a [Publication.Builder] to build a [Publication] from a publication asset.
     *
     * @param asset Digital medium (e.g. a file) used to access the publication.
     * @param fetcher Initial leaf fetcher which should be used to read the publication's resources.
     * This can be used to:
     * - support content protection technologies
     * - parse exploded archives or in archiving formats unknown to the parser, e.g. RAR
     * If the asset is not an archive, it will be reachable at the HREF /<asset.name>,
     * e.g. with a PDF.
     * @param warnings Used to report non-fatal parsing warnings, such as publication authoring
     * mistakes. This is useful to warn users of potential rendering issues or help authors
     * debug their publications.
     */
    suspend fun parse(asset: PublicationAsset, fetcher: Fetcher, warnings: WarningLogger? = null): Publication.Builder?

}
