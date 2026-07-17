/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import kotlin.test.assertEquals
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.AbsoluteUrl
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TdmTest {

    @Test
    fun `invalid policy is just ignored`() {
        assertEquals(
            Tdm(
                reservation = Tdm.Reservation.ALL,
                policy = null
            ),
            Tdm.fromJSON(
                JSONObject(
                    """{
                        "reservation": "all",
                        "policy": "not an URL"
                    }"""
                )
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
            Tdm.fromJSON(JSONObject("""{ "reservation": "none" }")"""))
        )
    }

    @Test
    fun `parse null JSON`() {
        Assert.assertNull(Tdm.fromJSON(null))
    }

    @Test
    fun `parse full JSON`() {
        assertEquals(
            Tdm(
                reservation = Tdm.Reservation.ALL,
                policy = AbsoluteUrl("https://policy")!!
            ),
            Tdm.fromJSON(
                JSONObject(
                    """{
                        "reservation": "all",
                        "policy": "https://policy"
                    }"""
                )
            )
        )
    }

    @Test
    fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("""{ "reservation": "all" }"""),
            Tdm(
                reservation = Tdm.Reservation.ALL,
                policy = null
            ).toJSON(),
        )
    }

    @Test
    fun `get full JSON`() {
        assertJSONEquals(
            JSONObject(
                """{
                    "reservation": "all",
                    "policy": "https://policy"
                }"""
            ),
            Tdm(
                reservation = Tdm.Reservation.ALL,
                policy = AbsoluteUrl("https://policy")!!
            ).toJSON(),
        )
    }
}
