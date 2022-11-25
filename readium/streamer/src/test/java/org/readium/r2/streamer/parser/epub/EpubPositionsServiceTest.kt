/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceTry
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Try
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpubPositionsServiceTest {

    @Test
    fun `Positions from an empty {readingOrder}`() {
        val service = createService(readingOrder = emptyList())

        assertEquals(0, runBlocking { service.positions().size })
    }

    @Test
    fun `Positions  from a {readingOrder} with one resource`() {
        val service = createService(
            readingOrder = listOf(
                Pair(1L, Link(href = "res", type = "application/xml"))
            )
        )

        assertEquals(
            listOf(
                Locator(
                    href = "res",
                    type = "application/xml",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 1,
                        totalProgression = 0.0
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `Positions from a {readingOrder} with a few resources`() {
        val service = createService(
            readingOrder = listOf(
                Pair(1L, Link(href = "res")),
                Pair(2L, Link(href = "chap1", type = "application/xml")),
                Pair(2L, Link(href = "chap2", type = "text/html", title = "Chapter 2"))
            )
        )

        assertEquals(
            listOf(
                Locator(
                    href = "res",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = "chap1",
                    type = "application/xml",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 2,
                        totalProgression = 1.0 / 3.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    title = "Chapter 2",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 3,
                        totalProgression = 2.0 / 3.0
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `{type} fallbacks on text-html`() {
        val service = createService(
            readingOrder = listOf(
                Pair(1L, Link(href = "chap1", properties = createProperties(layout = EpubLayout.REFLOWABLE))),
                Pair(1L, Link(href = "chap2", properties = createProperties(layout = EpubLayout.FIXED)))
            )
        )

        assertEquals(
            listOf(
                Locator(
                    href = "chap1",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 2,
                        totalProgression = 0.5
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `One position per fixed-layout resources`() {
        val service = createService(
            layout = EpubLayout.FIXED,
            readingOrder = listOf(
                Pair(10000L, Link(href = "res")),
                Pair(20000L, Link(href = "chap1", type = "application/xml")),
                Pair(40000L, Link(href = "chap2", type = "text/html", title = "Chapter 2"))
            )
        )

        assertEquals(
            listOf(
                Locator(
                    href = "res",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = "chap1",
                    type = "application/xml",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 2,
                        totalProgression = 1.0 / 3.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    title = "Chapter 2",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 3,
                        totalProgression = 2.0 / 3.0
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `Split reflowable resources by the provided number of bytes`() {
        val service = createService(
            layout = EpubLayout.REFLOWABLE,
            readingOrder = listOf(
                Pair(0L, Link(href = "chap1")),
                Pair(49L, Link(href = "chap2", type = "application/xml")),
                Pair(50L, Link(href = "chap3", type = "text/html", title = "Chapter 3")),
                Pair(51L, Link(href = "chap4")),
                Pair(120L, Link(href = "chap5"))
            ),
            reflowableStrategy = EpubPositionsService.ReflowableStrategy.ArchiveEntryLength(pageLength = 50)
        )

        assertEquals(
            listOf(
                Locator(
                    href = "chap1",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "application/xml",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 2,
                        totalProgression = 1.0 / 8.0
                    )
                ),
                Locator(
                    href = "chap3",
                    type = "text/html",
                    title = "Chapter 3",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 3,
                        totalProgression = 2.0 / 8.0
                    )
                ),
                Locator(
                    href = "chap4",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 4,
                        totalProgression = 3.0 / 8.0
                    )
                ),
                Locator(
                    href = "chap4",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.5,
                        position = 5,
                        totalProgression = 4.0 / 8.0
                    )
                ),
                Locator(
                    href = "chap5",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 6,
                        totalProgression = 5.0 / 8.0
                    )
                ),
                Locator(
                    href = "chap5",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 1.0 / 3.0,
                        position = 7,
                        totalProgression = 6.0 / 8.0
                    )
                ),
                Locator(
                    href = "chap5",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 2.0 / 3.0,
                        position = 8,
                        totalProgression = 7.0 / 8.0
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `{layout} fallbacks to reflowable`() {
        // We check this by verifying that the resource will be split every 1024 bytes
        val service = createService(
            layout = null,
            readingOrder = listOf(
                Pair(60L, Link(href = "chap1"))
            ),
            reflowableStrategy = EpubPositionsService.ReflowableStrategy.ArchiveEntryLength(pageLength = 50)
        )

        assertEquals(
            listOf(
                Locator(
                    href = "chap1",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = "chap1",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.5,
                        position = 2,
                        totalProgression = 0.5
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `Positions from publication with mixed layouts`() {
        val service = createService(
            layout = EpubLayout.FIXED,
            readingOrder = listOf(
                Pair(20000L, Link(href = "chap1")),
                Pair(60L, Link(href = "chap2", properties = createProperties(layout = EpubLayout.REFLOWABLE))),
                Pair(20000L, Link(href = "chap3", properties = createProperties(layout = EpubLayout.FIXED)))
            ),
            reflowableStrategy = EpubPositionsService.ReflowableStrategy.ArchiveEntryLength(pageLength = 50)
        )

        assertEquals(
            listOf(
                Locator(
                    href = "chap1",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 2,
                        totalProgression = 1.0 / 4.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.5,
                        position = 3,
                        totalProgression = 2.0 / 4.0
                    )
                ),
                Locator(
                    href = "chap3",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 4,
                        totalProgression = 3.0 / 4.0
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    @Test
    fun `Use the {ArchiveEntryLength} reflowable strategy`() {
        val service = createService(
            layout = EpubLayout.REFLOWABLE,
            readingOrder = listOf(
                Pair(60L, Link(href = "chap1", properties = createProperties(archiveEntryLength = 20L))),
                Pair(60L, Link(href = "chap2"))
            ),
            reflowableStrategy = EpubPositionsService.ReflowableStrategy.ArchiveEntryLength(pageLength = 50)
        )

        assertEquals(
            listOf(
                listOf(
                    Locator(
                        href = "chap1",
                        type = "text/html",
                        locations = Locator.Locations(
                            progression = 0.0,
                            position = 1,
                            totalProgression = 0.0
                        )
                    ),
                ),
                listOf(
                    Locator(
                        href = "chap2",
                        type = "text/html",
                        locations = Locator.Locations(
                            progression = 0.0,
                            position = 2,
                            totalProgression = 1.0 / 3.0
                        )
                    ),
                    Locator(
                        href = "chap2",
                        type = "text/html",
                        locations = Locator.Locations(
                            progression = 0.5,
                            position = 3,
                            totalProgression = 2.0 / 3.0
                        )
                    )
                )
            ),
            runBlocking { service.positionsByReadingOrder() }
        )
    }

    @Test
    fun `Use the {OriginalLength} reflowable strategy`() {
        val service = createService(
            layout = EpubLayout.REFLOWABLE,
            readingOrder = listOf(
                Pair(60L, Link(href = "chap1", properties = createProperties(originalLength = 20L))),
                Pair(60L, Link(href = "chap2"))
            ),
            reflowableStrategy = EpubPositionsService.ReflowableStrategy.OriginalLength(pageLength = 50)
        )

        assertEquals(
            listOf(
                Locator(
                    href = "chap1",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 1,
                        totalProgression = 0.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 2,
                        totalProgression = 1.0 / 3.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.5,
                        position = 3,
                        totalProgression = 2.0 / 3.0
                    )
                )
            ),
            runBlocking { service.positions() }
        )
    }

    private fun createService(
        layout: EpubLayout? = null,
        readingOrder: List<Pair<Long, Link>>,
        reflowableStrategy: EpubPositionsService.ReflowableStrategy = EpubPositionsService.ReflowableStrategy.ArchiveEntryLength(pageLength = 50)
    ) = EpubPositionsService(
        readingOrder = readingOrder.map { it.second },
        fetcher = object : Fetcher {

            private fun findResource(relativePath: String): Pair<Long, Link>? =
                readingOrder.find { it.second.href == relativePath }

            override suspend fun links(): List<Link> = emptyList()

            override fun get(link: Link): Resource = object : Resource {
                override suspend fun link(): Link = link

                override suspend fun length() = findResource(link.href)
                    ?.let { Try.success(it.first) }
                    ?: Try.failure(Resource.Exception.NotFound())

                override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = Try.success(ByteArray(0))

                override suspend fun close() {}
            }

            override suspend fun close() {}
        },
        presentation = Presentation(layout = layout),
        reflowableStrategy = reflowableStrategy
    )

    private fun createProperties(
        layout: EpubLayout? = null,
        archiveEntryLength: Long? = null,
        originalLength: Long? = null
    ): Properties {
        val properties = mutableMapOf<String, Any>()
        if (layout != null) {
            properties["layout"] = layout.value
        }
        if (originalLength != null) {
            properties["encrypted"] = mapOf(
                "algorithm" to "algo",
                "originalLength" to originalLength
            )
        }
        if (archiveEntryLength != null) {
            properties["archive"] = mapOf(
                "entryLength" to archiveEntryLength,
                "isEntryCompressed" to true
            )
        }
        return Properties(otherProperties = properties)
    }
}
