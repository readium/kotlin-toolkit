/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import org.json.JSONObject
import org.junit.Test
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.extensions.optNullableInt
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
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
            override val positionsByReadingOrder: List<List<Locator>> = positions
        }

        val json = service.get(Link("/~readium/positions"))
            ?.readAsString()
            ?.successOrNull()
            ?.let {
                JSONObject(it)
            }
        val total = json
            ?.optNullableInt("total")
        val locators = json
            ?.optJSONArray("positions")
            ?.mapNotNull { (it as? JSONObject)?.let {
                Locator.fromJSON(
                    it
                )
            } }

        assertEquals(positions.flatten().size, total)
        assertEquals(positions.flatten(), locators)
    }

    @Test
    fun `helper for ServicesBuilder works fine`() {
        val factory = { context: Publication.Service.Context ->
            object : PositionsService {
                override val positionsByReadingOrder: List<List<Locator>> = emptyList()
            }
        }
        assertEquals(
            mapOf<String, ServiceFactory>(PositionsService::class.simpleName!! to factory),
            Publication.ServicesBuilder().apply { positionsServiceFactory = factory }.serviceFactories
        )
    }
}
