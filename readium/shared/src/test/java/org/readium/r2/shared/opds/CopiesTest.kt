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

class CopiesTest {

    @Test fun `parse minimal JSON copies`() {
        assertEquals(
            Copies(total = null, available = null),
            Copies.fromJSON(JSONObject("{}"))
        )
    }

    @Test fun `parse full JSON copies`() {
        assertEquals(
            Copies(total = 5, available = 6),
            Copies.fromJSON(JSONObject("{'total': 5, 'available': 6}"))
        )
    }

    @Test fun `parse null JSON copies`() {
        assertNull(Copies.fromJSON(null))
    }

    @Test fun `parse JSON copies requires positive {total}`() {
        assertEquals(
            Copies(total = null, available = 6),
            Copies.fromJSON(JSONObject("{'total': -5, 'available': 6}"))
        )
    }

    @Test fun `parse JSON copies requires positive {available}`() {
        assertEquals(
            Copies(total = 5, available = null),
            Copies.fromJSON(JSONObject("{'total': 5, 'available': -6}"))
        )
    }

    @Test fun `get minimal JSON copies`() {
        assertJSONEquals(
            JSONObject("{}"),
            Copies(total = null, available = null).toJSON()
        )
    }

    @Test fun `get full JSON copies`() {
        assertJSONEquals(
            JSONObject("{'total': 5, 'available': 6}"),
            Copies(total = 5, available = 6).toJSON()
        )
    }

}
