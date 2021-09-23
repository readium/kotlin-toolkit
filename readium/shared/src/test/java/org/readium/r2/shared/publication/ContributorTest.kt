/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.toJSON

class ContributorTest {

    @Test fun `parse JSON string`() {
        assertEquals(
            Contributor(localizedName = LocalizedString("Thom Yorke")),
            Contributor.fromJSON("Thom Yorke")
        )
    }

    @Test fun `parse minimal JSON`() {
        assertEquals(
            Contributor(localizedName = LocalizedString("Colin Greenwood")),
            Contributor.fromJSON(JSONObject("{'name': 'Colin Greenwood'}"))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            Contributor(
                localizedName = LocalizedString("Colin Greenwood"),
                localizedSortAs = LocalizedString("greenwood"),
                identifier = "colin",
                roles = setOf("bassist"),
                position = 4.0,
                links = listOf(
                    Link(href = "http://link1"),
                    Link(href = "http://link2")
                )
            ),
            Contributor.fromJSON(JSONObject("""{
                "name": "Colin Greenwood",
                "identifier": "colin",
                "sortAs": "greenwood",
                "role": "bassist",
                "position": 4,
                "links": [
                    {"href": "http://link1"},
                    {"href": "http://link2"}
                ]
            }"""))
        )
    }

    @Test fun `parse JSON with multiple roles`() {
        assertEquals(
            Contributor(
                localizedName = LocalizedString("Thom Yorke"),
                roles = setOf("singer", "guitarist")
            ),
            Contributor.fromJSON(JSONObject("""{
                "name": "Thom Yorke",
                "role": ["singer", "guitarist", "guitarist"]
            }"""))
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(Contributor.fromJSON(null))
    }

    @Test fun `parse requires {name}`() {
        assertNull(Contributor.fromJSON(JSONObject("{'identifier': 'c1'}")))
    }

    @Test fun `parse JSON array`() {
        assertEquals(
            listOf(
                Contributor(localizedName = LocalizedString("Thom Yorke")),
                Contributor(
                    localizedName = LocalizedString.fromStrings(mapOf(
                        "en" to "Jonny Greenwood",
                        "fr" to "Jean Boisvert"
                    )),
                    roles = setOf("guitarist")
                )
            ),
            Contributor.fromJSONArray(JSONArray("""[
                "Thom Yorke",
                {
                    "name": {"en": "Jonny Greenwood", "fr": "Jean Boisvert"},
                    "role": "guitarist"
                }
            ]"""))
        )
    }

    @Test fun `parse null JSON array`() {
        assertEquals(0, Contributor.fromJSONArray(null).size)
    }

    @Test fun `parse JSON array ignores invalid contributors`() {
        assertEquals(
            listOf(
                Contributor(localizedName = LocalizedString("Thom Yorke"))
            ),
            Contributor.fromJSONArray(JSONArray("""[
                "Thom Yorke",
                {
                    "role": "guitarist"
                }
            ]"""))
        )
    }

    @Test fun `parse array from string`() {
        assertEquals(
            listOf(Contributor(localizedName = LocalizedString("Thom Yorke"))),
            Contributor.fromJSONArray("Thom Yorke")
        )
    }

    @Test fun `parse array from single object`() {
        assertEquals(
            listOf(Contributor(
                localizedName = LocalizedString.fromStrings(mapOf(
                    "en" to "Jonny Greenwood",
                    "fr" to "Jean Boisvert"
                )),
                roles = setOf("guitarist")
            )),
            Contributor.fromJSONArray(JSONObject("""{
                "name": {"en": "Jonny Greenwood", "fr": "Jean Boisvert"},
                "role": "guitarist"
            }"""))
        )
    }

    @Test fun `get name from the default translation`() {
        assertEquals(
            "Jonny Greenwood",
            Contributor(localizedName = LocalizedString.fromStrings(mapOf(
                "en" to "Jonny Greenwood",
                "fr" to "Jean Boisvert"
            ))).name
        )

    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("{'name': {'und': 'Colin Greenwood'}}"),
            Contributor(localizedName = LocalizedString("Colin Greenwood"))
                .toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "name": {"und": "Colin Greenwood"},
                "identifier": "colin",
                "sortAs": {"und": "greenwood"},
                "role": ["bassist"],
                "position": 4.0,
                "links": [
                    {"href": "http://link1", "templated": false},
                    {"href": "http://link2", "templated": false}
                ]
            }"""),
            Contributor(
                localizedName = LocalizedString("Colin Greenwood"),
                localizedSortAs = LocalizedString("greenwood"),
                identifier = "colin",
                roles = setOf("bassist"),
                position = 4.0,
                links = listOf(
                    Link(href = "http://link1"),
                    Link(href = "http://link2")
                )
            ).toJSON()
        )
    }

    @Test fun `get JSON array`() {
        assertJSONEquals(
            JSONArray("""[
                {
                    "name": {"und": "Thom Yorke"},
                },
                {
                    "name": {"en": "Jonny Greenwood", "fr": "Jean Boisvert"},
                    "role": ["guitarist"]
                }
            ]"""),
            listOf(
                Contributor(localizedName = LocalizedString("Thom Yorke")),
                Contributor(
                    localizedName = LocalizedString.fromStrings(mapOf(
                        "en" to "Jonny Greenwood",
                        "fr" to "Jean Boisvert"
                    )),
                    roles = setOf("guitarist")
                )
            ).toJSON()
        )
    }

}
