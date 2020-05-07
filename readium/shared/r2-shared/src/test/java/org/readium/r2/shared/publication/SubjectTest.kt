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

class SubjectTest {

    @Test fun `parse JSON string`() {
        assertEquals(
            Subject(localizedName = LocalizedString("Fantasy")),
            Subject.fromJSON("Fantasy")
        )
    }

    @Test fun `parse minimal JSON`() {
        assertEquals(
            Subject(localizedName = LocalizedString("Science Fiction")),
            Subject.fromJSON(JSONObject("{'name': 'Science Fiction'}"))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            Subject(
                localizedName = LocalizedString("Science Fiction"),
                localizedSortAs = LocalizedString("science-fiction"),
                scheme = "http://scheme",
                code = "CODE",
                links = listOf(
                    Link(href = "pub1"),
                    Link(href = "pub2")
                )
            ),
            Subject.fromJSON(JSONObject("""{
                "name": "Science Fiction",
                "sortAs": "science-fiction",
                "scheme": "http://scheme",
                "code": "CODE",
                "links": [
                    {"href": "pub1"},
                    {"href": "pub2"}
                ]
            }"""))
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(Subject.fromJSON(null))
    }

    @Test fun `parse requires {name}`() {
        assertNull(Subject.fromJSON(JSONObject("{'sortAs': 'science-fiction'}")))
    }

    @Test fun `parse JSON array`() {
        assertEquals(
            listOf(
                Subject(localizedName = LocalizedString("Fantasy")),
                Subject(
                    localizedName = LocalizedString("Science Fiction"),
                    scheme = "http://scheme"
                )
            ),
            Subject.fromJSONArray(JSONArray("""[
                "Fantasy",
                {
                    "name": "Science Fiction",
                    "scheme": "http://scheme"
                }
            ]"""))
        )
    }

    @Test fun `parse null JSON array`() {
        assertEquals(0, Subject.fromJSONArray(null).size)
    }

    @Test fun `parse JSON array ignores invalid subjects`() {
        assertEquals(
            listOf(
                Subject(localizedName = LocalizedString("Fantasy"))
            ),
            Subject.fromJSONArray(JSONArray("""[
                "Fantasy",
                {
                    "code": "CODE"
                }
            ]"""))
        )
    }

    @Test fun `parse array from string`() {
        assertEquals(
            listOf(Subject(localizedName = LocalizedString("Fantasy"))),
            Subject.fromJSONArray("Fantasy")
        )
    }

    @Test fun `parse array from single object`() {
        assertEquals(
            listOf(Subject(localizedName = LocalizedString("Fantasy"), code = "CODE")),
            Subject.fromJSONArray(JSONObject("""{
                "name": "Fantasy",
                "code": "CODE"
            }"""))
        )
    }

    @Test fun `get name from the default translation`() {
        assertEquals(
            "Hello world",
            Subject(localizedName = LocalizedString.fromStrings(mapOf(
                "en" to "Hello world",
                "fr" to "Salut le monde"
            ))).name
        )

    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("{'name': {'und': 'Science Fiction'}}"),
            Subject(localizedName = LocalizedString("Science Fiction"))
                .toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "name": {"und": "Science Fiction"},
                "sortAs": {"und": "science-fiction"},
                "scheme": "http://scheme",
                "code": "CODE",
                "links": [
                    {"href": "pub1", "templated": false},
                    {"href": "pub2", "templated": false}
                ]
            }"""),
            Subject(
                localizedName = LocalizedString("Science Fiction"),
                localizedSortAs = LocalizedString("science-fiction"),
                scheme = "http://scheme",
                code = "CODE",
                links = listOf(
                    Link(href = "pub1"),
                    Link(href = "pub2")
                )
            ).toJSON()
        )
    }

    @Test fun `get JSON array`() {
        assertJSONEquals(
            JSONArray("""[
                {
                    "name": {"und": "Fantasy"},
                },
                {
                    "name": {"und": "Science Fiction"},
                    "scheme": "http://scheme"
                }
            ]"""),
            listOf(
                Subject(localizedName = LocalizedString("Fantasy")),
                Subject(
                    localizedName = LocalizedString("Science Fiction"),
                    scheme = "http://scheme"
                )
            ).toJSON()
        )
    }

}
