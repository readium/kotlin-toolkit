/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
package org.readium.r2.streamer.parser.pdf

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

class PdfPositionsServiceTest {

    @Test
    fun `Positions from 0 pages`() {
        val service = createService(pageCount = 0)

        assertEquals(0, runBlocking { service.positions().size })
    }

    @Test
    fun `Positions from 1 page`() {
        val service = createService(pageCount = 1)

        assertEquals(
            listOf(listOf(Locator(
                href = "/publication.pdf",
                type = "application/pdf",
                locations = Locator.Locations(
                    fragments = listOf("page=1"),
                    progression = 0.0,
                    position = 1,
                    totalProgression = 0.0
                )
            ))),
            runBlocking { service.positionsByReadingOrder() }
        )
    }

    @Test
    fun `Positions from several pages`() {
        val service = createService(
            pageCount = 3
        )

        assertEquals(
            listOf(
                listOf(
                    Locator(
                        href = "/publication.pdf",
                        type = "application/pdf",
                        locations = Locator.Locations(
                            fragments = listOf("page=1"),
                            progression = 0.0,
                            position = 1,
                            totalProgression = 0.0
                        )
                    ),
                    Locator(
                        href = "/publication.pdf",
                        type = "application/pdf",
                        locations = Locator.Locations(
                            fragments = listOf("page=2"),
                            progression = 1.0 / 3.0,
                            position = 2,
                            totalProgression = 1.0 / 3.0
                        )
                    ),
                    Locator(
                        href = "/publication.pdf",
                        type = "application/pdf",
                        locations = Locator.Locations(
                            fragments = listOf("page=3"),
                            progression = 2.0 / 3.0,
                            position = 3,
                            totalProgression = 2.0 / 3.0
                        )
                    )
                )
            ),
            runBlocking { service.positionsByReadingOrder() }
        )
    }

    private fun createService(
        link: Link = Link(href = "/publication.pdf"),
        pageCount: Int
    ) = PdfPositionsService(
        link = link,
        pageCount = pageCount,
        tableOfContents = emptyList()
    )

}