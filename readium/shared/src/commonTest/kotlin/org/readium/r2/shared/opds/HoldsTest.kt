/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.opds

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class HoldsTest {

    @Test fun `parse minimal JSON holds`() {
        assertEquals(
            Holds(total = null, position = null),
            Holds.fromJSON("{}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse full JSON holds`() {
        assertEquals(
            Holds(total = 5, position = 6),
            Holds.fromJSON("{\"total\": 5, \"position\": 6}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse null JSON holds`() {
        assertNull(Holds.fromJSON(null))
    }

    @Test fun `parse JSON holds requires positive total`() {
        assertEquals(
            Holds(total = null, position = 6),
            Holds.fromJSON("{\"total\": -5, \"position\": 6}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse JSON holds requires positive position`() {
        assertEquals(
            Holds(total = 5, position = null),
            Holds.fromJSON("{\"total\": 5, \"position\": -6}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `get minimal JSON holds`() {
        assertJSONEquals(
            "{}".toJsonObjectOrNull()!!,
            Holds(total = null, position = null).toJSON()
        )
    }

    @Test fun `get full JSON holds`() {
        assertJSONEquals(
            "{\"total\": 5, \"position\": 6}".toJsonObjectOrNull()!!,
            Holds(total = 5, position = 6).toJSON()
        )
    }
}
