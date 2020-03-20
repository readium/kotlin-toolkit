/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.streamer.container.Container
import java.io.ByteArrayInputStream
import java.io.InputStream

class EpubPositionListFactoryTest {

    @Test
    fun `Create from an empty {readingOrder}`() {
        val factory = createFactory(readingOrder = emptyList())

        assertEquals(0, factory.create().size)
    }

    @Test
    fun `Create from a {readingOrder} with one resource`() {
        val factory = createFactory(
            readingOrder = listOf(
                Pair(1L, Link(href = "res", type = "application/xml"))
            )
        )

        assertEquals(
            listOf(Locator(
                href = "res",
                type = "application/xml",
                locations = Locator.Locations(
                    progression = 0.0,
                    position = 1,
                    totalProgression = 0.0
                )
            )),
            factory.create()
        )
    }

    @Test
    fun `Create from a {readingOrder} with a few resources`() {
        val factory = createFactory(
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
                        totalProgression = 1.0/3.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    title = "Chapter 2",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 3,
                        totalProgression = 2.0/3.0
                    )
                )
            ),
            factory.create()
        )
    }

    @Test
    fun `{type} fallbacks on text-html`() {
        val factory = createFactory(
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
            factory.create()
        )
    }

    @Test
    fun `Create one position per fixed-layout resources`() {
        val factory = createFactory(
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
                        totalProgression = 1.0/3.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    title = "Chapter 2",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 3,
                        totalProgression = 2.0/3.0
                    )
                )
            ),
            factory.create()
        )
    }

    @Test
    fun `Split reflowable resources by the provided number of bytes`() {
        val factory = createFactory(
            layout = EpubLayout.REFLOWABLE,
            readingOrder = listOf(
                Pair(0L, Link(href = "chap1")),
                Pair(49L, Link(href = "chap2", type = "application/xml")),
                Pair(50L, Link(href = "chap3", type = "text/html", title = "Chapter 3")),
                Pair(51L, Link(href = "chap4")),
                Pair(120L, Link(href = "chap5"))
            ),
            reflowablePositionLength = 50L
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
                        totalProgression = 1.0/8.0
                    )
                ),
                Locator(
                    href = "chap3",
                    type = "text/html",
                    title = "Chapter 3",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 3,
                        totalProgression = 2.0/8.0
                    )
                ),
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
                ),
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
           ),
            factory.create()
        )
    }

    @Test
    fun `{layout} fallbacks to reflowable`() {
        // We check this by verifying that the resource will be split every 1024 bytes
        val factory = createFactory(
            layout = null,
            readingOrder = listOf(
                Pair(60L, Link(href = "chap1"))
            ),
            reflowablePositionLength = 50L
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
            factory.create()
        )
    }

    @Test
    fun `Create from publication with mixed layouts`() {
        val factory = createFactory(
            layout = EpubLayout.FIXED,
            readingOrder = listOf(
                Pair(20000L, Link(href = "chap1")),
                Pair(60L, Link(href = "chap2", properties = createProperties(layout = EpubLayout.REFLOWABLE))),
                Pair(20000L, Link(href = "chap3", properties = createProperties(layout = EpubLayout.FIXED)))
            ),
            reflowablePositionLength = 50L
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
                        totalProgression = 1.0/4.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.5,
                        position = 3,
                        totalProgression = 2.0/4.0
                    )
                ),
                Locator(
                    href = "chap3",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.0,
                        position = 4,
                        totalProgression = 3.0/4.0
                    )
                )
            ),
            factory.create()
        )
    }

    @Test
    fun `Use the encrypted {originalLength} if available, instead of the {Container}'s file length`() {
        val factory = createFactory(
            layout = EpubLayout.REFLOWABLE,
            readingOrder = listOf(
                Pair(60L, Link(href = "chap1", properties = createProperties(encryptedOriginalLength = 20L))),
                Pair(60L, Link(href = "chap2"))
            ),
            reflowablePositionLength = 50L
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
                        totalProgression = 1.0/3.0
                    )
                ),
                Locator(
                    href = "chap2",
                    type = "text/html",
                    locations = Locator.Locations(
                        progression = 0.5,
                        position = 3,
                        totalProgression = 2.0/3.0
                    )
                )
            ),
            factory.create()
        )
    }

    private fun createFactory(
        layout: EpubLayout? = null,
        readingOrder: List<Pair<Long, Link>>,
        reflowablePositionLength: Long = 50L
    ) = EpubPositionListFactory(
        readingOrder = readingOrder.map { it.second },
        container = object : Container {
            override var rootFile: RootFile = RootFile()
            override var drm: DRM? = null

            override fun data(relativePath: String): ByteArray =
                ByteArray(0)

            override fun dataLength(relativePath: String): Long =
                findResource(relativePath)?.first ?: 0

            override fun dataInputStream(relativePath: String): InputStream =
                ByteArrayInputStream(data(relativePath))

            private fun findResource(relativePath: String): Pair<Long, Link>? =
                readingOrder.find { it.second.href == relativePath }
        },
        presentation = Presentation(layout = layout),
        reflowablePositionLength = reflowablePositionLength
    )

    private fun createProperties(layout: EpubLayout? = null, encryptedOriginalLength: Long? = null): Properties {
        val properties = mutableMapOf<String, Any>()
        if (layout != null) {
            properties["layout"] = layout.value
        }
        if (encryptedOriginalLength != null) {
            properties["encrypted"] = mapOf(
                "algorithm" to "algo",
                "originalLength" to encryptedOriginalLength
            )
        }

        return Properties(otherProperties = properties)
    }

}
