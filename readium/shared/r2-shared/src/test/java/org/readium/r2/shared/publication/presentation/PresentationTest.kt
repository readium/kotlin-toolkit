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
                clipped = false,
                continuous = true,
                fit = Presentation.Fit.CONTAIN,
                orientation = Presentation.Orientation.AUTO,
                overflow = Presentation.Overflow.AUTO,
                spread = Presentation.Spread.AUTO,
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
            JSONObject("""{
                "clipped": false,
                "continuous": true,
                "fit": "contain",
                "orientation": "auto",
                "overflow": "auto",
                "spread": "auto"
            }"""),
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
        assertEquals(Presentation.Fit.WIDTH, Presentation.Fit.from("width"))
        assertEquals(Presentation.Fit.HEIGHT, Presentation.Fit.from("height"))
        assertEquals(Presentation.Fit.CONTAIN, Presentation.Fit.from("contain"))
        assertEquals(Presentation.Fit.COVER, Presentation.Fit.from("cover"))

        // fallbacks
        assertEquals(Presentation.Fit.CONTAIN, Presentation.Fit.from("foobar"))
        assertEquals(Presentation.Fit.CONTAIN, Presentation.Fit.from(null))
    }

    @Test fun `get fit JSON value`() {
        assertEquals("width", Presentation.Fit.WIDTH.value)
        assertEquals("height", Presentation.Fit.HEIGHT.value)
        assertEquals("contain", Presentation.Fit.CONTAIN.value)
        assertEquals("cover", Presentation.Fit.COVER.value)
    }

    @Test fun `parse orientation from JSON value`() {
        assertEquals(Presentation.Orientation.AUTO, Presentation.Orientation.from("auto"))
        assertEquals(Presentation.Orientation.LANDSCAPE, Presentation.Orientation.from("landscape"))
        assertEquals(Presentation.Orientation.PORTRAIT, Presentation.Orientation.from("portrait"))

        // fallbacks
        assertEquals(Presentation.Orientation.AUTO, Presentation.Orientation.from("foobar"))
        assertEquals(Presentation.Orientation.AUTO, Presentation.Orientation.from(null))
    }

    @Test fun `get orientation JSON value`() {
        assertEquals("auto", Presentation.Orientation.AUTO.value)
        assertEquals("landscape", Presentation.Orientation.LANDSCAPE.value)
        assertEquals("portrait", Presentation.Orientation.PORTRAIT.value)
    }

    @Test fun `parse overflow from JSON value`() {
        assertEquals(Presentation.Overflow.AUTO, Presentation.Overflow.from("auto"))
        assertEquals(Presentation.Overflow.PAGINATED, Presentation.Overflow.from("paginated"))
        assertEquals(Presentation.Overflow.SCROLLED, Presentation.Overflow.from("scrolled"))

        // fallbacks
        assertEquals(Presentation.Overflow.AUTO, Presentation.Overflow.from("foobar"))
        assertEquals(Presentation.Overflow.AUTO, Presentation.Overflow.from(null))
    }

    @Test fun `get overflow JSON value`() {
        assertEquals("auto", Presentation.Overflow.AUTO.value)
        assertEquals("paginated", Presentation.Overflow.PAGINATED.value)
        assertEquals("scrolled", Presentation.Overflow.SCROLLED.value)
    }

    @Test fun `parse page from JSON value`() {
        assertEquals(Presentation.Page.LEFT, Presentation.Page.from("left"))
        assertEquals(Presentation.Page.RIGHT, Presentation.Page.from("right"))
        assertEquals(Presentation.Page.CENTER, Presentation.Page.from("center"))
        assertNull(Presentation.Page.from("foobar"))
        assertNull(Presentation.Page.from(null))
    }

    @Test fun `get page JSON value`() {
        assertEquals("left", Presentation.Page.LEFT.value)
        assertEquals("right", Presentation.Page.RIGHT.value)
        assertEquals("center", Presentation.Page.CENTER.value)
    }

    @Test fun `parse spread from JSON value`() {
        assertEquals(Presentation.Spread.AUTO, Presentation.Spread.from("auto"))
        assertEquals(Presentation.Spread.BOTH, Presentation.Spread.from("both"))
        assertEquals(Presentation.Spread.NONE, Presentation.Spread.from("none"))
        assertEquals(Presentation.Spread.LANDSCAPE, Presentation.Spread.from("landscape"))

        // fallbacks
        assertEquals(Presentation.Spread.AUTO, Presentation.Spread.from("foobar"))
        assertEquals(Presentation.Spread.AUTO, Presentation.Spread.from(null))
    }

    @Test fun `get spread JSON value`() {
        assertEquals("auto", Presentation.Spread.AUTO.value)
        assertEquals("both", Presentation.Spread.BOTH.value)
        assertEquals("none", Presentation.Spread.NONE.value)
        assertEquals("landscape", Presentation.Spread.LANDSCAPE.value)
    }

}
