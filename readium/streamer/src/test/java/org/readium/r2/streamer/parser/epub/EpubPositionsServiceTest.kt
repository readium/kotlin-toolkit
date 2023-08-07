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
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.resource.ArchiveProperties
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.archive
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
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
                ReadingOrderItem(href = "res", length = 1, type = MediaType.XML)
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
                ReadingOrderItem("res", length = 1),
                ReadingOrderItem("chap1", length = 2, MediaType.XML),
                ReadingOrderItem("chap2", length = 2, MediaType.HTML, title = "Chapter 2")
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
                ReadingOrderItem("chap1", length = 1, layout = EpubLayout.REFLOWABLE),
                ReadingOrderItem("chap2", length = 1, layout = EpubLayout.FIXED)
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
                ReadingOrderItem("res", length = 10000),
                ReadingOrderItem("chap1", length = 20000, MediaType.XML),
                ReadingOrderItem("chap2", length = 40000, MediaType.HTML, title = "Chapter 2")
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
                ReadingOrderItem("chap1", length = 0),
                ReadingOrderItem("chap2", length = 49, MediaType.XML),
                ReadingOrderItem("chap3", length = 50, MediaType.HTML, title = "Chapter 3"),
                ReadingOrderItem("chap4", length = 51),
                ReadingOrderItem("chap5", length = 120)
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
                ReadingOrderItem("chap1", length = 60)
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
                ReadingOrderItem("chap1", length = 20000),
                ReadingOrderItem("chap2", length = 60, layout = EpubLayout.REFLOWABLE),
                ReadingOrderItem("chap3", length = 20000, layout = EpubLayout.FIXED)
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
                ReadingOrderItem("chap1", length = 60, archiveEntryLength = 20L),
                ReadingOrderItem("chap2", length = 60)
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
                ReadingOrderItem("chap1", length = 60, originalLength = 20L),
                ReadingOrderItem("chap2", length = 60)
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
        readingOrder: List<ReadingOrderItem>,
        reflowableStrategy: EpubPositionsService.ReflowableStrategy = EpubPositionsService.ReflowableStrategy.ArchiveEntryLength(pageLength = 50)
    ) = EpubPositionsService(
        readingOrder = readingOrder.map { it.link },
        container = object : Container {

            private fun find(relativePath: String): ReadingOrderItem? =
                readingOrder.find { it.link.href == relativePath }

            override suspend fun entries(): Iterable<Container.Entry> = emptyList()

            override fun get(path: String): Container.Entry {
                val item = requireNotNull(find(path))

                return object : Container.Entry {
                    override val path: String = path

                    override val source: Url? = null

                    override suspend fun mediaType(): ResourceTry<MediaType?> =
                        Try.success(item.link.mediaType)

                    override suspend fun properties(): ResourceTry<Resource.Properties> =
                        Try.success(item.resourceProperties)

                    override suspend fun length() = Try.success(item.length)

                    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
                        Try.success(ByteArray(0))

                    override suspend fun close() {}
                }
            }

            override suspend fun close() {}
        },
        presentation = Presentation(layout = layout),
        reflowableStrategy = reflowableStrategy
    )

    class ReadingOrderItem(
        val href: String,
        val length: Long,
        val type: MediaType? = null,
        val title: String? = null,
        val archiveEntryLength: Long? = null,
        val originalLength: Long? = null,
        val layout: EpubLayout? = null,
    ) {
        val link: Link = Link(
            href = href,
            type = type?.toString(),
            title = title,
            properties = Properties(
                buildMap {
                    if (layout != null) {
                        put("layout", layout.value)
                    }
                    if (originalLength != null) {
                        put(
                            "encrypted",
                            mapOf(
                                "algorithm" to "algo",
                                "originalLength" to originalLength
                            )
                        )
                    }
                }
            )
        )

        val resourceProperties: Resource.Properties = Resource.Properties {
            if (archiveEntryLength != null) {
                archive = ArchiveProperties(
                    entryLength = archiveEntryLength,
                    isEntryCompressed = true
                )
            }
        }
    }
}
