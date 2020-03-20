/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.html

import org.json.JSONObject
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DomRangeTest {

    @Test fun `parse {DomRange} minimal JSON`() {
        assertEquals(
            DomRange(start = DomRange.Point(cssSelector = "p", textNodeIndex = 4)),
            DomRange.fromJSON(JSONObject("""{
                "start": {
                    "cssSelector": "p",
                    "textNodeIndex": 4
                }
            }"""))
        )
    }

    @Test fun `parse {DomRange} full JSON`() {
        assertEquals(
            DomRange(
                start = DomRange.Point(cssSelector = "p", textNodeIndex = 4),
                end = DomRange.Point(cssSelector = "a", textNodeIndex = 2)
            ),
            DomRange.fromJSON(JSONObject("""{
                "start": {
                    "cssSelector": "p",
                    "textNodeIndex": 4
                },
                "end": {
                    "cssSelector": "a",
                    "textNodeIndex": 2
                }
            }"""))
        )
    }

    @Test fun `parse {DomRange} invalid JSON`() {
        assertNull(DomRange.fromJSON(JSONObject("{ 'invalid': 'object' }")))
    }

    @Test fun `parse {DomRange} null JSON`() {
        assertNull(DomRange.fromJSON(null))
    }

    @Test fun `get {DomRange} minimal JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "start": {
                    "cssSelector": "p",
                    "textNodeIndex": 4
                }
            }"""),
            DomRange(start = DomRange.Point(cssSelector = "p", textNodeIndex = 4)).toJSON()
        )
    }

    @Test fun `get {DomRange} full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "start": {
                    "cssSelector": "p",
                    "textNodeIndex": 4
                },
                "end": {
                    "cssSelector": "a",
                    "textNodeIndex": 2
                }
            }"""),
            DomRange(
                start = DomRange.Point(cssSelector = "p", textNodeIndex = 4),
                end = DomRange.Point(cssSelector = "a", textNodeIndex = 2)
            ).toJSON()
        )
    }

    @Test fun `parse {Point} minimal JSON`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 4),
            DomRange.Point.fromJSON(JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 4
            }"""))
        )
    }

    @Test fun `parse {Point} full JSON`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 4, charOffset = 32),
            DomRange.Point.fromJSON(JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 4,
                "charOffset": 32
            }"""))
        )
    }

    @Test fun `parse {Point} invalid JSON`() {
        assertNull(DomRange.Point.fromJSON(JSONObject("""{
            "cssSelector": "p"
        }""")))
    }

    @Test fun `parse {Point} null JSON`() {
        assertNull(DomRange.Point.fromJSON(null))
    }

    @Test fun `parse {Point} requires positive {textNodeIndex}`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 1),
            DomRange.Point.fromJSON(JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 1
            }"""))
        )
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 0),
            DomRange.Point.fromJSON(JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 0
            }"""))
        )
        assertNull(DomRange.fromJSON(JSONObject("""{
            "cssSelector": "p",
            "textNodeIndex": -1
        }""")))
    }

    @Test fun `parse {Point} requires positive {charOffset}`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 1, charOffset = 1),
            DomRange.Point.fromJSON(JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 1,
                "charOffset": 1
            }"""))
        )
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 1, charOffset = 0),
            DomRange.Point.fromJSON(JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 1,
                "charOffset": 0
            }"""))
        )
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 1),
            DomRange.Point.fromJSON(JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 1,
                "charOffset": -1
            }"""))
        )
    }

    @Test fun `get {Point} minimal JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 4
            }"""),
            DomRange.Point(cssSelector = "p", textNodeIndex = 4).toJSON()
        )
    }

    @Test fun `get {Point} full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "cssSelector": "p",
                "textNodeIndex": 4,
                "charOffset": 32
            }"""),
            DomRange.Point(cssSelector = "p", textNodeIndex = 4, charOffset = 32).toJSON()
        )
    }

}
