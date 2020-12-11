/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.extensions.optNullableInt
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import kotlin.test.assertEquals

class PositionsServiceTest {

    @Test
    fun `get works fine`() {
        val positions = listOf(
            listOf(
                Locator(
                    href = "res",
                    type = "application/xml",
                    locations = Locator.Locations(
                        position = 1,
                        totalProgression = 0.0
                    )
                )
            ),
            listOf(
                Locator(
                    href = "chap1",
                    type = "image/png",
                    locations = Locator.Locations(
                        position = 2,
                        totalProgression = 1.0 / 4.0
                    )
                )
            ),
            listOf(
                Locator(
                    href = "chap2",
                    type = "image/png",
                    title = "Chapter 2",
                    locations = Locator.Locations(
                        position = 3,
                        totalProgression = 3.0 / 4.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "image/png",
                    title = "Chapter 2.5",
                    locations = Locator.Locations(
                        position = 4,
                        totalProgression = 3.0 / 4.0
                    )
                )
            )
        )

        val service = object : PositionsService {
            override suspend fun positionsByReadingOrder(): List<List<Locator>> = positions
        }

        val t = service.get(Link("/~readium/positions"))
            ?.let { runBlocking { it.readAsString() } }

        val json = service.get(Link("/~readium/positions"))
            ?.let { runBlocking { it.readAsString() } }
            ?.getOrNull()
            ?.let { JSONObject(it) }
        val total = json
            ?.optNullableInt("total")
        val locators = json
            ?.optJSONArray("positions")
            ?.mapNotNull { locator ->
                (locator as? JSONObject)?.let { Locator.fromJSON(it) }
            }

        assertEquals(positions.flatten().size, total)
        assertEquals(positions.flatten(), locators)
    }

    @Test
    fun `helper for ServicesBuilder works fine`() {
        val factory = { context: Publication.Service.Context ->
            object : PositionsService {
                override suspend fun positionsByReadingOrder(): List<List<Locator>> = emptyList()
            }
        }
        assertEquals(
            factory,
            Publication.ServicesBuilder().apply { positionsServiceFactory = factory }.positionsServiceFactory
        )
    }
}

class PerResourcePositionsServiceTest {

    @Test
    fun `Positions from an empty {readingOrder}`() {
        val service = PerResourcePositionsService(readingOrder = emptyList(), fallbackMediaType = "")

        Assert.assertEquals(0, runBlocking { service.positions().size })
    }

    @Test
    fun `Positions from a {readingOrder} with one resource`() {
        val service = PerResourcePositionsService(
            readingOrder = listOf(Link(href = "res", type = "image/png")),
            fallbackMediaType = ""
        )

        Assert.assertEquals(
            listOf(Locator(
                href = "res",
                type = "image/png",
                locations = Locator.Locations(
                    position = 1,
                    totalProgression = 0.0
                )
            )),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `Positions from a {readingOrder} with a few resources`() {
        val service = PerResourcePositionsService(
            readingOrder = listOf(
                Link(href = "res"),
                Link(href = "chap1", type = "image/png"),
                Link(href = "chap2", type = "image/png", title = "Chapter 2")
            ),
            fallbackMediaType = ""
        )

        Assert.assertEquals(
            listOf(
                Locator(
                    href = "res",
                    type = "",
                    locations = Locator.Locations(
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = "chap1",
                    type = "image/png",
                    locations = Locator.Locations(
                        position = 2,
                        totalProgression = 1.0 / 3.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "image/png",
                    title = "Chapter 2",
                    locations = Locator.Locations(
                        position = 3,
                        totalProgression = 2.0 / 3.0
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `{type} fallbacks on the given media type`() {
        val services = PerResourcePositionsService(
            readingOrder = listOf(
                Link(href = "res")
            ),
            fallbackMediaType = "image/*"
        )

        Assert.assertEquals(
            listOf(Locator(
                href = "res",
                type = "image/*",
                locations = Locator.Locations(
                    position = 1,
                    totalProgression = 0.0
                )
            )),
            runBlocking { services.positions() }
        )
    }

}
