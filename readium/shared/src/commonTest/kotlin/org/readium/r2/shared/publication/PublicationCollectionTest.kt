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
import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class PublicationCollectionTest {

    @Test fun `parse minimal JSON`() {
        assertEquals(
            PublicationCollection(
                links = listOf(Link(href = Href("/link")!!))
            ),
            PublicationCollection.fromJSON(
                """{
                "metadata": {},
                "links": [{"href": "/link"}]
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            PublicationCollection(
                metadata = mapOf("metadata1" to "value"),
                links = listOf(Link(href = Href("/link")!!)),
                subcollections = mapOf(
                    "sub1" to listOf(
                        PublicationCollection(links = listOf(Link(href = Href("/sublink")!!)))
                    ),
                    "sub2" to listOf(
                        PublicationCollection(
                            links = listOf(
                                Link(href = Href("/sublink1")!!),
                                Link(href = Href("/sublink2")!!)
                            )
                        )
                    ),
                    "sub3" to listOf(
                        PublicationCollection(links = listOf(Link(href = Href("/sublink3")!!))),
                        PublicationCollection(links = listOf(Link(href = Href("/sublink4")!!)))
                    )
                )
            ),
            PublicationCollection.fromJSON(
                """{
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
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(PublicationCollection.fromJSON(null))
    }

    @Test fun `parse multiple JSON collections`() {
        assertEquals(
            mapOf(
                "sub1" to listOf(
                    PublicationCollection(links = listOf(Link(href = Href("/sublink")!!)))
                ),
                "sub2" to listOf(
                    PublicationCollection(
                        links = listOf(
                            Link(href = Href("/sublink1")!!),
                            Link(href = Href("/sublink2")!!)
                        )
                    )
                ),
                "sub3" to listOf(
                    PublicationCollection(links = listOf(Link(href = Href("/sublink3")!!))),
                    PublicationCollection(links = listOf(Link(href = Href("/sublink4")!!)))
                )
            ),
            PublicationCollection.collectionsFromJSON(
                """{
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
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            """{
                "metadata": {},
                "links": [{"href": "/link", "templated": false}]
            }""".toJsonObjectOrNull()!!,
            PublicationCollection(links = listOf(Link(href = Href("/link")!!))).toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            """{
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
                    ]
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
            }""".toJsonObjectOrNull()!!,
            PublicationCollection(
                metadata = mapOf("metadata1" to "value"),
                links = listOf(Link(href = Href("/link")!!)),
                subcollections = mapOf(
                    "sub1" to listOf(
                        PublicationCollection(links = listOf(Link(href = Href("/sublink")!!)))
                    ),
                    "sub2" to listOf(
                        PublicationCollection(
                            links = listOf(
                                Link(href = Href("/sublink1")!!),
                                Link(href = Href("/sublink2")!!)
                            )
                        )
                    ),
                    "sub3" to listOf(
                        PublicationCollection(links = listOf(Link(href = Href("/sublink3")!!))),
                        PublicationCollection(links = listOf(Link(href = Href("/sublink4")!!)))
                    )
                )
            ).toJSON()
        )
    }

    @Test fun `get multiple JSON collections`() {
        assertJSONEquals(
            """{
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
                    ]
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
            }""".toJsonObjectOrNull()!!,
            mapOf(
                "sub1" to listOf(
                    PublicationCollection(links = listOf(Link(href = Href("/sublink")!!)))
                ),
                "sub2" to listOf(
                    PublicationCollection(
                        links = listOf(
                            Link(href = Href("/sublink1")!!),
                            Link(href = Href("/sublink2")!!)
                        )
                    )
                ),
                "sub3" to listOf(
                    PublicationCollection(links = listOf(Link(href = Href("/sublink3")!!))),
                    PublicationCollection(links = listOf(Link(href = Href("/sublink4")!!)))
                )
            ).toJSONObject()
        )
    }
}
