/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.opds

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.toJSON

class AcquisitionTest {

    @Test fun `parse minimal JSON acquisition`() {
        assertEquals(
            Acquisition(type = "acquisition-type"),
            Acquisition.fromJSON(JSONObject("{'type': 'acquisition-type'}"))
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
            Acquisition.fromJSON(JSONObject("""{
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
            }"""))
        )
    }

    @Test fun `parse invalid JSON acquisition`() {
        assertNull(Acquisition.fromJSON(JSONObject("{}")))
    }

    @Test fun `parse null JSON acquisition`() {
        assertNull(Acquisition.fromJSON(null))
    }

    @Test fun `parse JSON acquisition requires {type}`() {
        assertNull(Acquisition.fromJSON(JSONObject("{'child': []}")))
    }

    @Test fun `parse JSON acquisition array`() {
        assertEquals(
            listOf(
                Acquisition(type = "acq1"),
                Acquisition(type = "acq2")
            ),
            Acquisition.fromJSONArray(JSONArray("""[
                { "type": "acq1" },
                { "type": "acq2" }
            ]"""))
        )
    }

    @Test fun `parse JSON acquisition array ignores invalid acquisitions`() {
        assertEquals(
            listOf(
                Acquisition(type = "acq1")
            ),
            Acquisition.fromJSONArray(JSONArray("""[
                { "type": "acq1" },
                { "invalid": "acq2" }
            ]"""))
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
            JSONObject("{'type': 'acquisition-type'}"),
            Acquisition(type = "acquisition-type").toJSON()
        )
    }

    @Test fun `get full JSON acquisition`() {
        assertJSONEquals(
            JSONObject("""{
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
            }"""),
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
            JSONArray("""[
                { "type": "acq1" },
                { "type": "acq2" }
            ]"""),
            listOf(
                Acquisition(type = "acq1"),
                Acquisition(type = "acq2")
            ).toJSON()
        )
    }

}