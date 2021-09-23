/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.presentation

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.publication.epub.EpubLayout

class PresentationTest {

    @Test fun `parse null JSON`() {
        assertEquals(Presentation(), Presentation.fromJSON(null))
    }

    @Test fun `parse minimal JSON`() {
        assertEquals(
            Presentation(
                clipped = null,
                continuous = null,
                fit = null,
                orientation = null,
                overflow = null,
                spread = null,
                layout = null
            ),
            Presentation.fromJSON(JSONObject("{}"))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            Presentation(
                clipped = true,
                continuous = false,
                fit = Presentation.Fit.COVER,
                orientation = Presentation.Orientation.LANDSCAPE,
                overflow = Presentation.Overflow.PAGINATED,
                spread = Presentation.Spread.BOTH,
                layout = EpubLayout.FIXED
            ),
            Presentation.fromJSON(JSONObject("""{
                "clipped": true,
                "continuous": false,
                "fit": "cover",
                "orientation": "landscape",
                "overflow": "paginated",
                "spread": "both",
                "layout": "fixed"
            }"""))
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("{}"),
            Presentation().toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "clipped": true,
                "continuous": false,
                "fit": "cover",
                "orientation": "landscape",
                "overflow": "paginated",
                "spread": "both",
                "layout": "fixed"
            }"""),
            Presentation(
                clipped = true,
                continuous = false,
                fit = Presentation.Fit.COVER,
                orientation = Presentation.Orientation.LANDSCAPE,
                overflow = Presentation.Overflow.PAGINATED,
                spread = Presentation.Spread.BOTH,
                layout = EpubLayout.FIXED
            ).toJSON()
        )
    }

    @Test fun `parse fit from JSON value`() {
        assertEquals(Presentation.Fit.WIDTH, Presentation.Fit("width"))
        assertEquals(Presentation.Fit.HEIGHT, Presentation.Fit("height"))
        assertEquals(Presentation.Fit.CONTAIN, Presentation.Fit("contain"))
        assertEquals(Presentation.Fit.COVER, Presentation.Fit("cover"))
        assertNull(Presentation.Fit("foobar"))
        assertNull(Presentation.Fit(null))
    }

    @Test fun `get fit JSON value`() {
        assertEquals("width", Presentation.Fit.WIDTH.value)
        assertEquals("height", Presentation.Fit.HEIGHT.value)
        assertEquals("contain", Presentation.Fit.CONTAIN.value)
        assertEquals("cover", Presentation.Fit.COVER.value)
    }

    @Test fun `parse orientation from JSON value`() {
        assertEquals(Presentation.Orientation.AUTO, Presentation.Orientation("auto"))
        assertEquals(Presentation.Orientation.LANDSCAPE, Presentation.Orientation("landscape"))
        assertEquals(Presentation.Orientation.PORTRAIT, Presentation.Orientation("portrait"))
        assertNull(Presentation.Orientation("foobar"))
        assertNull(Presentation.Orientation(null))
    }

    @Test fun `get orientation JSON value`() {
        assertEquals("auto", Presentation.Orientation.AUTO.value)
        assertEquals("landscape", Presentation.Orientation.LANDSCAPE.value)
        assertEquals("portrait", Presentation.Orientation.PORTRAIT.value)
    }

    @Test fun `parse overflow from JSON value`() {
        assertEquals(Presentation.Overflow.AUTO, Presentation.Overflow("auto"))
        assertEquals(Presentation.Overflow.PAGINATED, Presentation.Overflow("paginated"))
        assertEquals(Presentation.Overflow.SCROLLED, Presentation.Overflow("scrolled"))
        assertNull(Presentation.Overflow("foobar"))
        assertNull(Presentation.Overflow(null))
    }

    @Test fun `get overflow JSON value`() {
        assertEquals("auto", Presentation.Overflow.AUTO.value)
        assertEquals("paginated", Presentation.Overflow.PAGINATED.value)
        assertEquals("scrolled", Presentation.Overflow.SCROLLED.value)
    }

    @Test fun `parse page from JSON value`() {
        assertEquals(Presentation.Page.LEFT, Presentation.Page("left"))
        assertEquals(Presentation.Page.RIGHT, Presentation.Page("right"))
        assertEquals(Presentation.Page.CENTER, Presentation.Page("center"))
        assertNull(Presentation.Page("foobar"))
        assertNull(Presentation.Page(null))
    }

    @Test fun `get page JSON value`() {
        assertEquals("left", Presentation.Page.LEFT.value)
        assertEquals("right", Presentation.Page.RIGHT.value)
        assertEquals("center", Presentation.Page.CENTER.value)
    }

    @Test fun `parse spread from JSON value`() {
        assertEquals(Presentation.Spread.AUTO, Presentation.Spread("auto"))
        assertEquals(Presentation.Spread.BOTH, Presentation.Spread("both"))
        assertEquals(Presentation.Spread.NONE, Presentation.Spread("none"))
        assertEquals(Presentation.Spread.LANDSCAPE, Presentation.Spread("landscape"))
        assertNull(Presentation.Spread("foobar"))
        assertNull(Presentation.Spread(null))
    }

    @Test fun `get spread JSON value`() {
        assertEquals("auto", Presentation.Spread.AUTO.value)
        assertEquals("both", Presentation.Spread.BOTH.value)
        assertEquals("none", Presentation.Spread.NONE.value)
        assertEquals("landscape", Presentation.Spread.LANDSCAPE.value)
    }

}
