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

class CopiesTest {

    @Test fun `parse minimal JSON copies`() {
        assertEquals(
            Copies(total = null, available = null),
            Copies.fromJSON("{}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse full JSON copies`() {
        assertEquals(
            Copies(total = 5, available = 6),
            Copies.fromJSON("{\"total\": 5, \"available\": 6}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse null JSON copies`() {
        assertNull(Copies.fromJSON(null))
    }

    @Test fun `parse JSON copies requires positive total`() {
        assertEquals(
            Copies(total = null, available = 6),
            Copies.fromJSON("{\"total\": -5, \"available\": 6}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse JSON copies requires positive available`() {
        assertEquals(
            Copies(total = 5, available = null),
            Copies.fromJSON("{\"total\": 5, \"available\": -6}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `get minimal JSON copies`() {
        assertJSONEquals(
            "{}".toJsonObjectOrNull()!!,
            Copies(total = null, available = null).toJSON()
        )
    }

    @Test fun `get full JSON copies`() {
        assertJSONEquals(
            "{\"total\": 5, \"available\": 6}".toJsonObjectOrNull()!!,
            Copies(total = 5, available = 6).toJSON()
        )
    }
}
