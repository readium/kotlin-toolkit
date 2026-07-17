/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication.html

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class DomRangeTest {

    @Test fun `parse DomRange minimal JSON`() {
        assertEquals(
            DomRange(start = DomRange.Point(cssSelector = "p", textNodeIndex = 4)),
            DomRange.fromJSON(
                """{
                "start": {
                    "cssSelector": "p",
                    "textNodeIndex": 4
                }
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse DomRange full JSON`() {
        assertEquals(
            DomRange(
                start = DomRange.Point(cssSelector = "p", textNodeIndex = 4),
                end = DomRange.Point(cssSelector = "a", textNodeIndex = 2)
            ),
            DomRange.fromJSON(
                """{
                "start": {
                    "cssSelector": "p",
                    "textNodeIndex": 4
                },
                "end": {
                    "cssSelector": "a",
                    "textNodeIndex": 2
                }
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse DomRange invalid JSON`() {
        assertNull(DomRange.fromJSON("{ \"invalid\": \"object\" }".toJsonObjectOrNull()!!))
    }

    @Test fun `parse DomRange null JSON`() {
        assertNull(DomRange.fromJSON(null))
    }

    @Test fun `get DomRange minimal JSON`() {
        assertJSONEquals(
            """{
                "start": {
                    "cssSelector": "p",
                    "textNodeIndex": 4
                }
            }""".toJsonObjectOrNull()!!,
            DomRange(start = DomRange.Point(cssSelector = "p", textNodeIndex = 4)).toJSON()
        )
    }

    @Test fun `get DomRange full JSON`() {
        assertJSONEquals(
            """{
                "start": {
                    "cssSelector": "p",
                    "textNodeIndex": 4
                },
                "end": {
                    "cssSelector": "a",
                    "textNodeIndex": 2
                }
            }""".toJsonObjectOrNull()!!,
            DomRange(
                start = DomRange.Point(cssSelector = "p", textNodeIndex = 4),
                end = DomRange.Point(cssSelector = "a", textNodeIndex = 2)
            ).toJSON()
        )
    }

    @Test fun `parse Point minimal JSON`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 4),
            DomRange.Point.fromJSON(
                """{
                "cssSelector": "p",
                "textNodeIndex": 4
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse Point full JSON`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 4, charOffset = 32),
            DomRange.Point.fromJSON(
                """{
                "cssSelector": "p",
                "textNodeIndex": 4,
                "charOffset": 32
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    /**
     * `offset` got replaced with `charOffset`, but we still need to be able to parse it to ensure
     * backward-compatibility for apps which persisted legacy versions of the DomRange model.
     */
    @Test fun `parse legacy Point JSON`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 4, charOffset = 32),
            DomRange.Point.fromJSON(
                """{
                "cssSelector": "p",
                "textNodeIndex": 4,
                "offset": 32
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse Point invalid JSON`() {
        assertNull(
            DomRange.Point.fromJSON(
                """{
            "cssSelector": "p"
        }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse Point null JSON`() {
        assertNull(DomRange.Point.fromJSON(null))
    }

    @Test fun `parse Point requires positive textNodeIndex`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 1),
            DomRange.Point.fromJSON(
                """{
                "cssSelector": "p",
                "textNodeIndex": 1
            }""".toJsonObjectOrNull()!!
            )
        )
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 0),
            DomRange.Point.fromJSON(
                """{
                "cssSelector": "p",
                "textNodeIndex": 0
            }""".toJsonObjectOrNull()!!
            )
        )
        assertNull(
            DomRange.fromJSON(
                """{
            "cssSelector": "p",
            "textNodeIndex": -1
        }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse Point requires positive charOffset`() {
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 1, charOffset = 1),
            DomRange.Point.fromJSON(
                """{
                "cssSelector": "p",
                "textNodeIndex": 1,
                "charOffset": 1
            }""".toJsonObjectOrNull()!!
            )
        )
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 1, charOffset = 0),
            DomRange.Point.fromJSON(
                """{
                "cssSelector": "p",
                "textNodeIndex": 1,
                "charOffset": 0
            }""".toJsonObjectOrNull()!!
            )
        )
        assertEquals(
            DomRange.Point(cssSelector = "p", textNodeIndex = 1),
            DomRange.Point.fromJSON(
                """{
                "cssSelector": "p",
                "textNodeIndex": 1,
                "charOffset": -1
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `get Point minimal JSON`() {
        assertJSONEquals(
            """{
                "cssSelector": "p",
                "textNodeIndex": 4
            }""".toJsonObjectOrNull()!!,
            DomRange.Point(cssSelector = "p", textNodeIndex = 4).toJSON()
        )
    }

    @Test fun `get Point full JSON`() {
        assertJSONEquals(
            """{
                "cssSelector": "p",
                "textNodeIndex": 4,
                "charOffset": 32
            }""".toJsonObjectOrNull()!!,
            DomRange.Point(cssSelector = "p", textNodeIndex = 4, charOffset = 32).toJSON()
        )
    }
}
