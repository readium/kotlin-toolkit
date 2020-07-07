/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals

class PublicationCollectionTest {

    @Test fun `parse minimal JSON`() {
        assertEquals(
            PublicationCollection(
                links = listOf(Link(href = "/link"))),
            PublicationCollection.fromJSON(JSONObject("""{
                "metadata": {},
                "links": [{"href": "/link"}]
            }"""))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            PublicationCollection(
                metadata = mapOf("metadata1" to "value"),
                links = listOf(Link(href = "/link")),
                subcollections = mapOf(
                    "sub1" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink")))),
                    "sub2" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink1"), Link(href = "/sublink2")))),
                    "sub3" to listOf(
                        PublicationCollection(links = listOf(Link(href = "/sublink3"))),
                        PublicationCollection(links = listOf(Link(href = "/sublink4")))
                    )
                )
            ),
            PublicationCollection.fromJSON(JSONObject("""{
                "metadata": {
                    "metadata1": "value"
                },
                "links": [
                    {"href": "/link"}
                ],
                "sub1": {
                    "links": [
                        {"href": "/sublink"}
                    ]
                },
                "sub2": [
                    {"href": "/sublink1"},
                    {"href": "/sublink2"}
                ],
                "sub3": [
                    {
                        "links": [
                            {"href": "/sublink3"}
                        ]
                    },
                    {
                        "links": [
                            {"href": "/sublink4"}
                        ]
                    }
                ]
            }"""))
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(PublicationCollection.fromJSON(null))
    }

    @Test fun `parse multiple JSON collections`() {
        assertEquals(
            mapOf(
                "sub1" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink")))),
                "sub2" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink1"), Link(href = "/sublink2")))),
                "sub3" to listOf(
                    PublicationCollection(links = listOf(Link(href = "/sublink3"))),
                    PublicationCollection(links = listOf(Link(href = "/sublink4")))
                )
            ),
            PublicationCollection.collectionsFromJSON(JSONObject("""{
                "sub1": {
                    "links": [
                        {"href": "/sublink"}
                    ]
                },
                "sub2": [
                    {"href": "/sublink1"},
                    {"href": "/sublink2"}
                ],
                "sub3": [
                    {
                        "links": [
                            {"href": "/sublink3"}
                        ]
                    },
                    {
                        "links": [
                            {"href": "/sublink4"}
                        ]
                    }
                ]
            }"""))
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "metadata": {},
                "links": [{"href": "/link", "templated": false}]
            }"""),
            PublicationCollection(links = listOf(Link(href = "/link"))).toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "metadata": {
                    "metadata1": "value"
                },
                "links": [
                    {"href": "/link", "templated": false}
                ],
                "sub1": {
                    "metadata": {},
                    "links": [
                        {"href": "/sublink", "templated": false}
                    ]
                },
                "sub2": {
                    "metadata": {},
                    "links": [
                        {"href": "/sublink1", "templated": false},
                        {"href": "/sublink2", "templated": false}
                    ],
                },
                "sub3": [
                    {
                        "metadata": {},
                        "links": [
                            {"href": "/sublink3", "templated": false}
                        ]
                    },
                    {
                        "metadata": {},
                        "links": [
                            {"href": "/sublink4", "templated": false}
                        ]
                    }
                ]
            }"""),
            PublicationCollection(
                metadata = mapOf("metadata1" to "value"),
                links = listOf(Link(href = "/link")),
                subcollections = mapOf(
                    "sub1" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink")))),
                    "sub2" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink1"), Link(href = "/sublink2")))),
                    "sub3" to listOf(
                        PublicationCollection(links = listOf(Link(href = "/sublink3"))),
                        PublicationCollection(links = listOf(Link(href = "/sublink4")))
                    )
                )
            ).toJSON()
        )
    }

    @Test fun `get multiple JSON collections`() {
        assertJSONEquals(
            JSONObject("""{
                "sub1": {
                    "metadata": {},
                    "links": [
                        {"href": "/sublink", "templated": false}
                    ]
                },
                "sub2": {
                    "metadata": {},
                    "links": [
                        {"href": "/sublink1", "templated": false},
                        {"href": "/sublink2", "templated": false}
                    ],
                },
                "sub3": [
                    {
                        "metadata": {},
                        "links": [
                            {"href": "/sublink3", "templated": false}
                        ]
                    },
                    {
                        "metadata": {},
                        "links": [
                            {"href": "/sublink4", "templated": false}
                        ]
                    }
                ]
            }"""),
            mapOf(
                "sub1" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink")))),
                "sub2" to listOf(PublicationCollection(links = listOf(Link(href = "/sublink1"), Link(href = "/sublink2")))),
                "sub3" to listOf(
                    PublicationCollection(links = listOf(Link(href = "/sublink3"))),
                    PublicationCollection(links = listOf(Link(href = "/sublink4")))
                )
            ).toJSONObject()
        )
    }
}
