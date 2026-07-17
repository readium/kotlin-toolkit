/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonPrimitive
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.json.toJsonArrayOrNull
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class SubjectTest {

    @Test fun `parse JSON string`() {
        assertEquals(
            Subject(localizedName = LocalizedString("Fantasy")),
            Subject.fromJSON(JsonPrimitive("Fantasy"))
        )
    }

    @Test fun `parse minimal JSON`() {
        assertEquals(
            Subject(localizedName = LocalizedString("Science Fiction")),
            Subject.fromJSON("{\"name\": \"Science Fiction\"}".toJsonObjectOrNull()!!)
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
                    Link(href = Href("pub1")!!),
                    Link(href = Href("pub2")!!)
                )
            ),
            Subject.fromJSON(
                """{
                "name": "Science Fiction",
                "sortAs": "science-fiction",
                "scheme": "http://scheme",
                "code": "CODE",
                "links": [
                    {"href": "pub1"},
                    {"href": "pub2"}
                ]
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(Subject.fromJSON(null))
    }

    @Test fun `parse requires name`() {
        assertNull(Subject.fromJSON("{\"sortAs\": \"science-fiction\"}".toJsonObjectOrNull()!!))
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
            Subject.fromJSONArray(
                """[
                "Fantasy",
                {
                    "name": "Science Fiction",
                    "scheme": "http://scheme"
                }
            ]""".toJsonArrayOrNull()!!
            )
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
            Subject.fromJSONArray(
                """[
                "Fantasy",
                {
                    "code": "CODE"
                }
            ]""".toJsonArrayOrNull()!!
            )
        )
    }

    @Test fun `parse array from string`() {
        assertEquals(
            listOf(Subject(localizedName = LocalizedString("Fantasy"))),
            Subject.fromJSONArray(JsonPrimitive("Fantasy"))
        )
    }

    @Test fun `parse array from single object`() {
        assertEquals(
            listOf(Subject(localizedName = LocalizedString("Fantasy"), code = "CODE")),
            Subject.fromJSONArray(
                """{
                "name": "Fantasy",
                "code": "CODE"
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `get name from the default translation`() {
        assertEquals(
            "Hello world",
            Subject(
                localizedName = LocalizedString.fromStrings(
                    mapOf(
                        "en" to "Hello world",
                        "fr" to "Salut le monde"
                    )
                )
            ).name
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            "{\"name\": {\"und\": \"Science Fiction\"}}".toJsonObjectOrNull()!!,
            Subject(localizedName = LocalizedString("Science Fiction"))
                .toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            """{
                "name": {"und": "Science Fiction"},
                "sortAs": {"und": "science-fiction"},
                "scheme": "http://scheme",
                "code": "CODE",
                "links": [
                    {"href": "pub1", "templated": false},
                    {"href": "pub2", "templated": false}
                ]
            }""".toJsonObjectOrNull()!!,
            Subject(
                localizedName = LocalizedString("Science Fiction"),
                localizedSortAs = LocalizedString("science-fiction"),
                scheme = "http://scheme",
                code = "CODE",
                links = listOf(
                    Link(href = Href("pub1")!!),
                    Link(href = Href("pub2")!!)
                )
            ).toJSON()
        )
    }

    @Test fun `get JSON array`() {
        assertJSONEquals(
            """[
                {
                    "name": {"und": "Fantasy"}
                },
                {
                    "name": {"und": "Science Fiction"},
                    "scheme": "http://scheme"
                }
            ]""".toJsonArrayOrNull()!!,
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
