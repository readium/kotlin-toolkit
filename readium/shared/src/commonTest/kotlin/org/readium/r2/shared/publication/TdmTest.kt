/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class TdmTest {

    @Test
    fun `invalid policy is just ignored`() {
        assertEquals(
            Tdm(
                reservation = Tdm.Reservation.ALL,
                policy = null
            ),
            Tdm.fromJSON(
                """{
                        "reservation": "all",
                        "policy": "not an URL"
                    }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test
    fun `parse minimal JSON`() {
        assertEquals(
            Tdm(
                reservation = Tdm.Reservation.NONE,
                policy = null
            ),
            Tdm.fromJSON("""{ "reservation": "none" }""".toJsonObjectOrNull()!!)
        )
    }

    @Test
    fun `parse null JSON`() {
        assertNull(Tdm.fromJSON(null))
    }

    @Test
    fun `parse full JSON`() {
        assertEquals(
            Tdm(
                reservation = Tdm.Reservation.ALL,
                policy = AbsoluteUrl("https://policy")!!
            ),
            Tdm.fromJSON(
                """{
                        "reservation": "all",
                        "policy": "https://policy"
                    }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test
    fun `get minimal JSON`() {
        assertJSONEquals(
            """{ "reservation": "all" }""".toJsonObjectOrNull()!!,
            Tdm(
                reservation = Tdm.Reservation.ALL,
                policy = null
            ).toJSON(),
        )
    }

    @Test
    fun `get full JSON`() {
        assertJSONEquals(
            """{
                    "reservation": "all",
                    "policy": "https://policy"
                }""".toJsonObjectOrNull()!!,
            Tdm(
                reservation = Tdm.Reservation.ALL,
                policy = AbsoluteUrl("https://policy")!!
            ).toJSON(),
        )
    }
}
