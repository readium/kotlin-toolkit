/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.pdf

import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

/**
 * Creates the [positions] for an PDF [Publication].
 *
 * @param documentHref The HREF of the PDF document in the [Publication].
 * @param pageCount Total page count in the PDF document.
 * @param tableOfContents Table of contents used to compute the position titles.
 */
internal class PdfPositionListFactory(
    private val documentHref: String,
    private val pageCount: Int,
    private val tableOfContents: List<Link>
) : Publication.PositionListFactory {

    override fun create(): List<Locator> {
        // FIXME: Use the [tableOfContents] to generate the titles
        if (pageCount <= 0) {
            Timber.e("Invalid page count for a PDF document: $pageCount")
            return emptyList()
        }

        return (1..pageCount).map { position ->
            val progression = (position - 1) / pageCount.toDouble()
            Locator(
                href = documentHref,
                type = MediaType.PDF.toString(),
                locations = Locator.Locations(
                    fragments = listOf("page=$position"),
                    progression = progression,
                    totalProgression = progression,
                    position = position
                )
            )
        }
    }

}
