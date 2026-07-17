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
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.json.toJsonArrayOrNull
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class AcquisitionTest {

    @Test fun `parse minimal JSON acquisition`() {
        assertEquals(
            Acquisition(type = "acquisition-type"),
            Acquisition.fromJSON("{\"type\": \"acquisition-type\"}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse full JSON acquisition`() {
        assertEquals(
            Acquisition(
                type = "acquisition-type",
                children = listOf(
                    Acquisition(
                        type = "sub-acquisition",
                        children = listOf(
                            Acquisition(type = "sub-sub1"),
                            Acquisition(type = "sub-sub2")
                        )
                    )
                )
            ),
            Acquisition.fromJSON(
                """{
                "type": "acquisition-type",
                "child": [
                    {
                        "type": "sub-acquisition",
                        "child": [
                            { "type": "sub-sub1" },
                            { "type": "sub-sub2" }
                        ]
                    }
                ]
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse invalid JSON acquisition`() {
        assertNull(Acquisition.fromJSON("{}".toJsonObjectOrNull()!!))
    }

    @Test fun `parse null JSON acquisition`() {
        assertNull(Acquisition.fromJSON(null))
    }

    @Test fun `parse JSON acquisition requires type`() {
        assertNull(Acquisition.fromJSON("{\"child\": []}".toJsonObjectOrNull()!!))
    }

    @Test fun `parse JSON acquisition array`() {
        assertEquals(
            listOf(
                Acquisition(type = "acq1"),
                Acquisition(type = "acq2")
            ),
            Acquisition.fromJSONArray(
                """[
                { "type": "acq1" },
                { "type": "acq2" }
            ]""".toJsonArrayOrNull()!!
            )
        )
    }

    @Test fun `parse JSON acquisition array ignores invalid acquisitions`() {
        assertEquals(
            listOf(
                Acquisition(type = "acq1")
            ),
            Acquisition.fromJSONArray(
                """[
                { "type": "acq1" },
                { "invalid": "acq2" }
            ]""".toJsonArrayOrNull()!!
            )
        )
    }

    @Test fun `parse null JSON acquisition array`() {
        assertEquals(
            emptyList<Acquisition>(),
            Acquisition.fromJSONArray(null)
        )
    }

    @Test fun `get minimal JSON acquisition`() {
        assertJSONEquals(
            "{\"type\": \"acquisition-type\"}".toJsonObjectOrNull()!!,
            Acquisition(type = "acquisition-type").toJSON()
        )
    }

    @Test fun `get full JSON acquisition`() {
        assertJSONEquals(
            """{
                "type": "acquisition-type",
                "child": [
                    {
                        "type": "sub-acquisition",
                        "child": [
                            { "type": "sub-sub1" },
                            { "type": "sub-sub2" }
                        ]
                    }
                ]
            }""".toJsonObjectOrNull()!!,
            Acquisition(
                type = "acquisition-type",
                children = listOf(
                    Acquisition(
                        type = "sub-acquisition",
                        children = listOf(
                            Acquisition(type = "sub-sub1"),
                            Acquisition(type = "sub-sub2")
                        )
                    )
                )
            ).toJSON()
        )
    }

    @Test fun `get JSON acquisition array`() {
        assertJSONEquals(
            """[
                { "type": "acq1" },
                { "type": "acq2" }
            ]""".toJsonArrayOrNull()!!,
            listOf(
                Acquisition(type = "acq1"),
                Acquisition(type = "acq2")
            ).toJSON()
        )
    }
}
