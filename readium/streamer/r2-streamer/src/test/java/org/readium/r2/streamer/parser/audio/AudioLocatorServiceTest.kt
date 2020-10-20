/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.audio

import org.junit.Test
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import kotlin.test.*

class AudioLocatorServiceTest {

    @Test
    fun `locate(Locator) matching reading order HREF`() {
        val service = AudioLocatorService(listOf(
            Link("l1"),
            Link("l2")
        ))

        val locator = Locator("l1", type = "audio/mpeg", locations = Locator.Locations(totalProgression = 0.53))
        assertEquals(locator, service.locate(locator))
    }

    @Test
    fun `locate(Locator) returns null if no match`() {
        val service = AudioLocatorService(listOf(
            Link("l1"),
            Link("l2")
        ))

        val locator = Locator("l3", type = "audio/mpeg", locations = Locator.Locations(totalProgression = 0.53))
        assertNull(service.locate(locator))
    }

    @Test
    fun `locate(Locator) uses totalProgression`() {
        val service = AudioLocatorService(listOf(
            Link("l1", type = "audio/mpeg", duration = 100.0),
            Link("l2", type = "audio/mpeg", duration = 100.0)
        ))

        assertEquals(
            Locator("l1", type = "audio/mpeg", locations = Locator.Locations(
                fragments = listOf("t=98"),
                progression = 98/100.0,
                totalProgression = 0.49
            )),
            service.locate(Locator("wrong", type = "audio/mpeg", locations = Locator.Locations(totalProgression = 0.49)))
        )

        assertEquals(
            Locator("l2", type = "audio/mpeg", locations = Locator.Locations(
                fragments = listOf("t=0"),
                progression = 0.0,
                totalProgression = 0.5
            )),
            service.locate(Locator("wrong", type = "audio/mpeg", locations = Locator.Locations(totalProgression = 0.5)))
        )

        assertEquals(
            Locator("l2", type = "audio/mpeg", locations = Locator.Locations(
                fragments = listOf("t=2"),
                progression = 0.02,
                totalProgression = 0.51
            )),
            service.locate(Locator("wrong", type = "audio/mpeg", locations = Locator.Locations(totalProgression = 0.51)))
        )
    }

    @Test
    fun `locate(Locator) using totalProgression keeps "title" and "text"`() {
        val service = AudioLocatorService(listOf(
            Link("l1", type = "audio/mpeg", duration = 100.0),
            Link("l2", type = "audio/mpeg", duration = 100.0)
        ))

        assertEquals(
            Locator(
                "l1",
                type = "audio/mpeg",
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
                    "wrong",
                    type = "wrong-type",
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
    fun `locate progression`() {
        val service = AudioLocatorService(listOf(
            Link("l1", type = "audio/mpeg", duration = 100.0),
            Link("l2", type = "audio/mpeg", duration = 100.0)
        ))

        assertEquals(
            Locator("l1", type = "audio/mpeg", locations = Locator.Locations(
                fragments = listOf("t=0"),
                progression = 0.0,
                totalProgression = 0.0
            )),
            service.locate(progression = 0.0)
        )

        assertEquals(
            Locator("l1", type = "audio/mpeg", locations = Locator.Locations(
                fragments = listOf("t=98"),
                progression = 98/100.0,
                totalProgression = 0.49
            )),
            service.locate(progression = 0.49)
        )

        assertEquals(
            Locator("l2", type = "audio/mpeg", locations = Locator.Locations(
                fragments = listOf("t=0"),
                progression = 0.0,
                totalProgression = 0.5
            )),
            service.locate(progression = 0.5)
        )

        assertEquals(
            Locator("l2", type = "audio/mpeg", locations = Locator.Locations(
                fragments = listOf("t=2"),
                progression = 0.02,
                totalProgression = 0.51
            )),
            service.locate(progression = 0.51)
        )

        assertEquals(
            Locator("l2", type = "audio/mpeg", locations = Locator.Locations(
                fragments = listOf("t=100"),
                progression = 1.0,
                totalProgression = 1.0
            )),
            service.locate(progression = 1.0)
        )
    }

    @Test
    fun `locate invalid progression`() {
        val service = AudioLocatorService(listOf(
            Link("l1", type = "audio/mpeg", duration = 100.0),
            Link("l2", type = "audio/mpeg", duration = 100.0)
        ))

        assertNull(service.locate(progression = -0.5))
        assertNull(service.locate(progression = 1.5))
    }

}