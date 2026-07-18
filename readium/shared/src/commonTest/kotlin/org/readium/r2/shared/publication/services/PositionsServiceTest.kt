/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import kotlin.test.*
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

class PositionsServiceTest {

    @Test
    fun `helper for ServicesBuilder works fine`() {
        val factory = { _: Publication.Service.Context ->
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
    fun `Positions from an empty readingOrder`() = runTest {
        val service = PerResourcePositionsService(
            readingOrder = emptyList(),
            fallbackMediaType = MediaType.BINARY
        )

        assertEquals(0, service.positions().size)
    }

    @Test
    fun `Positions from a readingOrder with one resource`() = runTest {
        val service = PerResourcePositionsService(
            readingOrder = listOf(Link(href = Href("res")!!, mediaType = MediaType.PNG)),
            fallbackMediaType = MediaType.BINARY
        )

        assertEquals(
            listOf(
                Locator(
                    href = Url("res")!!,
                    mediaType = MediaType.PNG,
                    locations = Locator.Locations(
                        position = 1,
                        totalProgression = 0.0
                    )
                )
            ),
            service.positions()
        )
    }

    @Test
    fun `Positions from a readingOrder with a few resources`() = runTest {
        val service = PerResourcePositionsService(
            readingOrder = listOf(
                Link(href = Href("res")!!),
                Link(href = Href("chap1")!!, mediaType = MediaType.PNG),
                Link(href = Href("chap2")!!, mediaType = MediaType.PNG, title = "Chapter 2")
            ),
            fallbackMediaType = MediaType.BINARY
        )

        assertEquals(
            listOf(
                Locator(
                    href = Url("res")!!,
                    mediaType = MediaType.BINARY,
                    locations = Locator.Locations(
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = Url("chap1")!!,
                    mediaType = MediaType.PNG,
                    locations = Locator.Locations(
                        position = 2,
                        totalProgression = 1.0 / 3.0
                    )
                ),
                Locator(
                    href = Url("chap2")!!,
                    mediaType = MediaType.PNG,
                    title = "Chapter 2",
                    locations = Locator.Locations(
                        position = 3,
                        totalProgression = 2.0 / 3.0
                    )
                )
            ),
            service.positions()
        )
    }

    @Test
    fun `type fallbacks on the given media type`() = runTest {
        val services = PerResourcePositionsService(
            readingOrder = listOf(
                Link(href = Href("res")!!)
            ),
            fallbackMediaType = MediaType("image/*")!!
        )

        assertEquals(
            listOf(
                Locator(
                    href = Url("res")!!,
                    mediaType = MediaType("image/*")!!,
                    locations = Locator.Locations(
                        position = 1,
                        totalProgression = 0.0
                    )
                )
            ),
            services.positions()
        )
    }
}
