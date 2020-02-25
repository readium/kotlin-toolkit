/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.opds

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals

class HoldsTest {

    @Test fun `parse minimal JSON holds`() {
        assertEquals(
            Holds(total = null, position = null),
            Holds.fromJSON(JSONObject("{}"))
        )
    }

    @Test fun `parse full JSON holds`() {
        assertEquals(
            Holds(total = 5, position = 6),
            Holds.fromJSON(JSONObject("{'total': 5, 'position': 6}"))
        )
    }

    @Test fun `parse null JSON holds`() {
        assertNull(Holds.fromJSON(null))
    }

    @Test fun `parse JSON holds requires positive {total}`() {
        assertEquals(
            Holds(total = null, position = 6),
            Holds.fromJSON(JSONObject("{'total': -5, 'position': 6}"))
        )
    }

    @Test fun `parse JSON holds requires positive {position}`() {
        assertEquals(
            Holds(total = 5, position = null),
            Holds.fromJSON(JSONObject("{'total': 5, 'position': -6}"))
        )
    }

    @Test fun `get minimal JSON holds`() {
        assertJSONEquals(
            JSONObject("{}"),
            Holds(total = null, position = null).toJSON()
        )
    }

    @Test fun `get full JSON holds`() {
        assertJSONEquals(
            JSONObject("{'total': 5, 'position': 6}"),
            Holds(total = 5, position = 6).toJSON()
        )
    }

}
