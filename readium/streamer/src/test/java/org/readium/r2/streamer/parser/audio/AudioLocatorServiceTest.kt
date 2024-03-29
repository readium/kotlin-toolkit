/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.audio

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioLocatorServiceTest {

    @Test
    fun `locate(Locator) matching reading order HREF`() = runBlocking {
        val service = AudioLocatorService(
            listOf(
                Link(Href("l1")!!),
                Link(Href("l2")!!)
            )
        )

        val locator = Locator(
            Url("l1")!!,
            mediaType = MediaType.MP3,
            locations = Locator.Locations(totalProgression = 0.53)
        )
        assertEquals(locator, service.locate(locator))
    }

    @Test
    fun `locate(Locator) returns null if no match`() = runBlocking {
        val service = AudioLocatorService(
            listOf(
                Link(Href("l1")!!),
                Link(Href("l2")!!)
            )
        )

        val locator = Locator(
            Url("l3")!!,
            mediaType = MediaType.MP3,
            locations = Locator.Locations(totalProgression = 0.53)
        )
        assertNull(service.locate(locator))
    }

    @Test
    fun `locate(Locator) uses totalProgression`() = runBlocking {
        val service = AudioLocatorService(
            listOf(
                Link(Href("l1")!!, mediaType = MediaType.MP3, duration = 100.0),
                Link(Href("l2")!!, mediaType = MediaType.MP3, duration = 100.0)
            )
        )

        assertEquals(
            Locator(
                Url("l1")!!,
                mediaType = MediaType.MP3,
                locations = Locator.Locations(
                    fragments = listOf("t=98"),
                    progression = 98 / 100.0,
                    totalProgression = 0.49
                )
            ),
            service.locate(
                Locator(
                    Url("wrong")!!,
                    mediaType = MediaType.MP3,
                    locations = Locator.Locations(totalProgression = 0.49)
                )
            )
        )

        assertEquals(
            Locator(
                Url("l2")!!,
                mediaType = MediaType.MP3,
                locations = Locator.Locations(
                    fragments = listOf("t=0"),
                    progression = 0.0,
                    totalProgression = 0.5
                )
            ),
            service.locate(
                Locator(
                    Url("wrong")!!,
                    mediaType = MediaType.MP3,
                    locations = Locator.Locations(totalProgression = 0.5)
                )
            )
        )

        assertEquals(
            Locator(
                Url("l2")!!,
                mediaType = MediaType.MP3,
                locations = Locator.Locations(
                    fragments = listOf("t=2"),
                    progression = 0.02,
                    totalProgression = 0.51
                )
            ),
            service.locate(
                Locator(
                    Url("wrong")!!,
                    mediaType = MediaType.MP3,
                    locations = Locator.Locations(totalProgression = 0.51)
                )
            )
        )
    }

    @Test
    fun `locate(Locator) using totalProgression keeps title and text`() = runBlocking {
        val service = AudioLocatorService(
            listOf(
                Link(Href("l1")!!, mediaType = MediaType.MP3, duration = 100.0),
                Link(Href("l2")!!, mediaType = MediaType.MP3, duration = 100.0)
            )
        )

        assertEquals(
            Locator(
                Url("l1")!!,
                mediaType = MediaType.MP3,
                title = "Title",
                locations = Locator.Locations(
                    fragments = listOf("t=80"),
                    progression = 80 / 100.0,
                    totalProgression = 0.4
                ),
                text = Locator.Text(after = "after", before = "before", highlight = "highlight")
            ),
            service.locate(
                Locator(
                    Url("wrong")!!,
                    mediaType = MediaType.BINARY,
                    title = "Title",
                    locations = Locator.Locations(
                        fragments = listOf("ignored"),
                        progression = 0.5,
                        totalProgression = 0.4,
                        position = 42,
                        otherLocations = mapOf("other" to "location")
                    ),
                    text = Locator.Text(after = "after", before = "before", highlight = "highlight")
                )
            )
        )
    }

    @Test
    fun `locate progression`() = runBlocking {
        val service = AudioLocatorService(
            listOf(
                Link(Href("l1")!!, mediaType = MediaType.MP3, duration = 100.0),
                Link(Href("l2")!!, mediaType = MediaType.MP3, duration = 100.0)
            )
        )

        assertEquals(
            Locator(
                Url("l1")!!,
                mediaType = MediaType.MP3,
                locations = Locator.Locations(
                    fragments = listOf("t=0"),
                    progression = 0.0,
                    totalProgression = 0.0
                )
            ),
            service.locateProgression(0.0)
        )

        assertEquals(
            Locator(
                Url("l1")!!,
                mediaType = MediaType.MP3,
                locations = Locator.Locations(
                    fragments = listOf("t=98"),
                    progression = 98 / 100.0,
                    totalProgression = 0.49
                )
            ),
            service.locateProgression(0.49)
        )

        assertEquals(
            Locator(
                Url("l2")!!,
                mediaType = MediaType.MP3,
                locations = Locator.Locations(
                    fragments = listOf("t=0"),
                    progression = 0.0,
                    totalProgression = 0.5
                )
            ),
            service.locateProgression(0.5)
        )

        assertEquals(
            Locator(
                Url("l2")!!,
                mediaType = MediaType.MP3,
                locations = Locator.Locations(
                    fragments = listOf("t=2"),
                    progression = 0.02,
                    totalProgression = 0.51
                )
            ),
            service.locateProgression(0.51)
        )

        assertEquals(
            Locator(
                Url("l2")!!,
                mediaType = MediaType.MP3,
                locations = Locator.Locations(
                    fragments = listOf("t=100"),
                    progression = 1.0,
                    totalProgression = 1.0
                )
            ),
            service.locateProgression(1.0)
        )
    }

    @Test
    fun `locate invalid progression`() = runBlocking {
        val service = AudioLocatorService(
            listOf(
                Link(Href("l1")!!, mediaType = MediaType.MP3, duration = 100.0),
                Link(Href("l2")!!, mediaType = MediaType.MP3, duration = 100.0)
            )
        )

        assertNull(service.locateProgression(-0.5))
        assertNull(service.locateProgression(1.5))
    }
}
