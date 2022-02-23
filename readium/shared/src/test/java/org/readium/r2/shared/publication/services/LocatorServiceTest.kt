/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LocatorServiceTest {

    // locate(Locator) checks that the href exists.
    @Test
    fun `locate from Locator`() = runTest {
        val service = createService(readingOrder = listOf(
            Link(href = "chap1", type = "application/xml"),
            Link(href = "chap2", type = "application/xml"),
            Link(href = "chap3", type = "application/xml")
        ))
        val locator = Locator(href = "chap2", type = "text/html", text = Locator.Text(highlight = "Highlight"))
        assertEquals(locator, service.locate(locator))
    }

    @Test
    fun `locate from Locator with empty reading order`() = runTest {
        val service = createService(readingOrder = emptyList())
        val locator = Locator(href = "chap2", type = "text/html", text = Locator.Text(highlight = "Highlight"))
        assertNull(service.locate(locator))
    }

    @Test
    fun `locate from Locator not found`() = runTest {
        val service = createService(readingOrder = listOf(
            Link(href = "chap1", type = "application/xml"),
            Link(href = "chap3", type = "application/xml")
        ))
        val locator = Locator(href = "chap2", type = "text/html", text = Locator.Text(highlight = "Highlight"))
        assertNull(service.locate(locator))
    }

    @Test
    fun `locate from progression`() = runTest {
        val service = createService(positions = positionsFixtures)

        assertEquals(Locator(
            href = "chap1",
            type = "text/html",
            locations = Locator.Locations(
                progression = 0.0,
                totalProgression = 0.0,
                position = 1
            )
        ), service.locateProgression(0.0))

        assertEquals(Locator(
            href = "chap3",
            type = "text/html",
            title = "Chapter 3",
            locations = Locator.Locations(
                progression = 0.0,
                totalProgression = 2.0/8.0,
                position = 3
            )
        ), service.locateProgression(0.25))

        val chap5FirstTotalProg = 5.0/8.0
        val chap4FirstTotalProg = 3.0/8.0

        assertEquals(Locator(
            href = "chap4",
            type = "text/html",
            locations = Locator.Locations(
                progression = (0.4 - chap4FirstTotalProg) / (chap5FirstTotalProg - chap4FirstTotalProg),
                totalProgression = 0.4,
                position = 4
            )
        ), service.locateProgression(0.4))

        assertEquals(Locator(
            href = "chap4",
            type = "text/html",
            locations = Locator.Locations(
                progression = (0.55 - chap4FirstTotalProg) / (chap5FirstTotalProg - chap4FirstTotalProg),
                totalProgression = 0.55,
                position = 5
            )
        ), service.locateProgression(0.55))

        assertEquals(Locator(
            href = "chap5",
            type = "text/html",
            locations = Locator.Locations(
                progression = (0.9 - chap5FirstTotalProg) / (1.0 - chap5FirstTotalProg),
                totalProgression = 0.9,
                position = 8
            )
        ), service.locateProgression(0.9))

        assertEquals(Locator(
            href = "chap5",
            type = "text/html",
            locations = Locator.Locations(
                progression = 1.0,
                totalProgression = 1.0,
                position = 8
            )
        ), service.locateProgression(1.0))
    }

    @Test
    fun `locate from incorrect progression`() = runTest {
        val service = createService(positions = positionsFixtures)
        assertNull(service.locateProgression(-0.2))
        assertNull(service.locateProgression(1.2))
    }

    @Test
    fun `locate from progression with empty positions`() = runTest {
        val service = createService(positions = emptyList())
        assertNull(service.locateProgression(0.5))
    }

    private fun createService(
        readingOrder: List<Link> = emptyList(),
        positions: List<List<Locator>> = emptyList()
    ) = DefaultLocatorService(
        readingOrder = readingOrder,
        positionsByReadingOrder = { positions }
    )

    private var positionsFixtures = listOf(
        listOf(
            Locator(
                href = "chap1",
                type = "text/html",
                locations = Locator.Locations(
                    progression = 0.0,
                    position = 1,
                    totalProgression = 0.0
                )
            )
        ),
        listOf(
            Locator(
                href = "chap2",
                type = "application/xml",
                locations = Locator.Locations(
                    progression = 0.0,
                    position = 2,
                    totalProgression = 1.0/8.0
                )
            )
        ),
        listOf(
            Locator(
                href = "chap3",
                type = "text/html",
                title = "Chapter 3",
                locations = Locator.Locations(
                    progression = 0.0,
                    position = 3,
                    totalProgression = 2.0/8.0
                )
            )
        ),
        listOf(
            Locator(
                href = "chap4",
                type = "text/html",
                locations = Locator.Locations(
                    progression = 0.0,
                    position = 4,
                    totalProgression = 3.0/8.0
                )
            ),
            Locator(
                href = "chap4",
                type = "text/html",
                locations = Locator.Locations(
                    progression = 0.5,
                    position = 5,
                    totalProgression = 4.0/8.0
                )
            )
        ),
        listOf(
            Locator(
                href = "chap5",
                type = "text/html",
                locations = Locator.Locations(
                    progression = 0.0,
                    position = 6,
                    totalProgression = 5.0/8.0
                )
            ),
            Locator(
                href = "chap5",
                type = "text/html",
                locations = Locator.Locations(
                    progression = 1.0/3.0,
                    position = 7,
                    totalProgression = 6.0/8.0
                )
            ),
            Locator(
                href = "chap5",
                type = "text/html",
                locations = Locator.Locations(
                    progression = 2.0/3.0,
                    position = 8,
                    totalProgression = 7.0/8.0
                )
            )
        )
    )

}