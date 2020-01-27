/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
package org.readium.r2.shared.publication.webpub.metadata

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.webpub.LocalizedString
import org.readium.r2.shared.publication.webpub.link.Link
import org.readium.r2.shared.toJSON

class SubjectTest {

    @Test fun `parse JSON string`() {
        assertEquals(
            Subject(localizedName = LocalizedString().withTranslation("Fantasy")),
            Subject.fromJSON("Fantasy")
        )
    }

    @Test fun `parse minimal JSON`() {
        assertEquals(
            Subject(localizedName = LocalizedString().withTranslation("Science Fiction")),
            Subject.fromJSON(JSONObject("{'name': 'Science Fiction'}"))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            Subject(
                localizedName = LocalizedString().withTranslation("Science Fiction"),
                sortAs = "science-fiction",
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
                Subject(localizedName = LocalizedString().withTranslation("Fantasy")),
                Subject(
                    localizedName = LocalizedString().withTranslation("Science Fiction"),
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
                Subject(localizedName = LocalizedString().withTranslation("Fantasy"))
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
            listOf(Subject(localizedName = LocalizedString().withTranslation("Fantasy"))),
            Subject.fromJSONArray("Fantasy")
        )
    }

    @Test fun `parse array from single object`() {
        assertEquals(
            listOf(Subject(localizedName = LocalizedString().withTranslation("Fantasy"), code = "CODE")),
            Subject.fromJSONArray(JSONObject("""{
                "name": "Fantasy",
                "code": "CODE"
            }"""))
        )
    }

    @Test fun `get name from the default translation`() {
        assertEquals(
            "Hello world",
            Subject(localizedName = LocalizedString()
                .withTranslation("Hello world", "en")
                .withTranslation("Salut le monde", "fr")
            ).name
        )

    }

    @Test fun `get minimal JSON`() {
        assertEquals(
            JSONObject("{'name': {'UND': 'Science Fiction'}}").toString(),
            Subject(localizedName = LocalizedString().withTranslation("Science Fiction"))
                .toJSON().toString()
        )
    }

    @Test fun `get full JSON`() {
        assertEquals(
            JSONObject("""{
                "name": {"UND": "Science Fiction"},
                "sortAs": "science-fiction",
                "scheme": "http://scheme",
                "code": "CODE",
                "links": [
                    {"href": "pub1", "templated": false},
                    {"href": "pub2", "templated": false}
                ]
            }""").toString(),
            Subject(
                localizedName = LocalizedString().withTranslation("Science Fiction"),
                sortAs = "science-fiction",
                scheme = "http://scheme",
                code = "CODE",
                links = listOf(
                    Link(href = "pub1"),
                    Link(href = "pub2")
                )
            ).toJSON().toString()
        )
    }

    @Test fun `get JSON array`() {
        assertEquals(
            JSONArray("""[
                {
                    "name": {"UND": "Fantasy"},
                },
                {
                    "name": {"UND": "Science Fiction"},
                    "scheme": "http://scheme"
                }
            ]""").toString(),
            listOf(
                Subject(localizedName = LocalizedString().withTranslation("Fantasy")),
                Subject(
                    localizedName = LocalizedString().withTranslation("Science Fiction"),
                    scheme = "http://scheme"
                )
            ).toJSON().toString()
        )
    }

}