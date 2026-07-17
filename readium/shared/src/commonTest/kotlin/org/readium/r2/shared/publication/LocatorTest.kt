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
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.json.toJsonObjectOrNull
import org.readium.r2.shared.util.mediatype.MediaType

class LocatorTest {

    @Test fun `parse Locator minimal JSON`() {
        assertEquals(
            Locator(href = Url("http://locator")!!, mediaType = MediaType.HTML),
            Locator.fromJSON(
                """{
                "href": "http://locator",
                "type": "text/html"
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse Locator full JSON`() {
        assertEquals(
            Locator(
                href = Url("http://locator")!!,
                mediaType = MediaType.HTML,
                title = "My Locator",
                locations = Locator.Locations(position = 42),
                text = Locator.Text(highlight = "Excerpt")
            ),
            Locator.fromJSON(
                """{
                "href": "http://locator",
                "type": "text/html",
                "title": "My Locator",
                "locations": {
                    "position": 42
                },
                "text": {
                    "highlight": "Excerpt"
                }
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse Locator null JSON`() {
        assertEquals(null, Locator.fromJSON(null))
    }

    @Test fun `parse Locator invalid JSON`() {
        assertNull(Locator.fromJSON("{ \"invalid\": \"object\" }".toJsonObjectOrNull()!!))
    }

    @OptIn(DelicateReadiumApi::class)
    @Test
    fun `parse Locator with legacy HREF`() {
        val json = """
            {
                "href": "legacy href",
                "type": "text/html"
            }
        """.toJsonObjectOrNull()!!

        assertNull(Locator.fromJSON(json))

        assertEquals(
            Locator(href = Url("legacy%20href")!!, mediaType = MediaType.HTML),
            Locator.fromLegacyJSON(json)
        )
    }

    @Test fun `get Locator minimal JSON`() {
        assertJSONEquals(
            """{
                "href": "http://locator",
                "type": "text/html"
            }""".toJsonObjectOrNull()!!,
            Locator(href = Url("http://locator")!!, mediaType = MediaType.HTML).toJSON()
        )
    }

    @Test fun `get Locator full JSON`() {
        assertJSONEquals(
            """{
                "href": "http://locator",
                "type": "text/html",
                "title": "My Locator",
                "locations": {
                    "position": 42
                },
                "text": {
                    "highlight": "Excerpt"
                }
            }""".toJsonObjectOrNull()!!,
            Locator(
                href = Url("http://locator")!!,
                mediaType = MediaType.HTML,
                title = "My Locator",
                locations = Locator.Locations(position = 42),
                text = Locator.Text(highlight = "Excerpt")
            ).toJSON()
        )
    }

    @Test fun `copy a Locator with different Locations sub-properties`() {
        assertEquals(
            Locator(
                href = Url("http://locator")!!,
                mediaType = MediaType.HTML,
                locations = Locator.Locations(
                    fragments = listOf("p=4", "frag34"),
                    progression = 0.74,
                    position = 42,
                    totalProgression = 0.32,
                    otherLocations = mapOf("other" to "other-location")
                )
            ),
            Locator(
                href = Url("http://locator")!!,
                mediaType = MediaType.HTML,
                locations = Locator.Locations(position = 42, progression = 2.0)
            ).copyWithLocations(
                fragments = listOf("p=4", "frag34"),
                progression = 0.74,
                totalProgression = 0.32,
                otherLocations = mapOf("other" to "other-location")
            )
        )
    }

    @Test fun `copy a Locator with reset Locations sub-properties`() {
        assertEquals(
            Locator(
                href = Url("http://locator")!!,
                mediaType = MediaType.HTML,
                locations = Locator.Locations()
            ),
            Locator(
                href = Url("http://locator")!!,
                mediaType = MediaType.HTML,
                locations = Locator.Locations(position = 42, progression = 2.0)
            ).copyWithLocations(
                fragments = emptyList(),
                progression = null,
                position = null,
                totalProgression = null,
                otherLocations = emptyMap()
            )
        )
    }

    @Test fun `parse Locations minimal JSON`() {
        assertEquals(
            Locator.Locations(),
            Locator.Locations.fromJSON("{}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse Locations full JSON`() {
        assertEquals(
            Locator.Locations(
                fragments = listOf("p=4", "frag34"),
                progression = 0.74,
                position = 42,
                totalProgression = 0.32,
                otherLocations = mapOf("other" to "other-location")
            ),
            Locator.Locations.fromJSON(
                """{
                "fragments": ["p=4", "frag34"],
                "progression": 0.74,
                "totalProgression": 0.32,
                "position": 42,
                "other": "other-location"
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse Locations null JSON`() {
        assertEquals(Locator.Locations(), Locator.Locations.fromJSON(null))
    }

    @Test fun `parse Locations single fragment JSON`() {
        assertEquals(
            Locator.Locations(fragments = listOf("frag34")),
            Locator.Locations.fromJSON("{ \"fragment\": \"frag34\" }".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse Locations ignores position smaller than 1`() {
        assertEquals(
            Locator.Locations(position = 1),
            Locator.Locations.fromJSON("{ \"position\": 1 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(),
            Locator.Locations.fromJSON("{ \"position\": 0 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(),
            Locator.Locations.fromJSON("{ \"position\": -1 }".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse Locations ignores an uncoercible progression`() {
        assertNull(
            Locator.Locations.fromJSON("{ \"progression\": \"abc\" }".toJsonObjectOrNull()!!).progression
        )
    }

    @Test fun `get Locations JSON drops otherLocations keys shadowed by null properties`() {
        assertJSONEquals(
            "{ \"other\": \"value\" }".toJsonObjectOrNull()!!,
            Locator.Locations(
                progression = null,
                otherLocations = mapOf("progression" to 0.5, "other" to "value")
            ).toJSON()
        )
    }

    @Test fun `parse Locations ignores progression outside of 0-1 range`() {
        assertEquals(
            Locator.Locations(progression = 0.5),
            Locator.Locations.fromJSON("{ \"progression\": 0.5 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(progression = 0.0),
            Locator.Locations.fromJSON("{ \"progression\": 0 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(progression = 1.0),
            Locator.Locations.fromJSON("{ \"progression\": 1 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(),
            Locator.Locations.fromJSON("{ \"progression\": -0.5 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(),
            Locator.Locations.fromJSON("{ \"progression\": 1.2 }".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse Locations ignores totalProgression outside of 0-1 range`() {
        assertEquals(
            Locator.Locations(totalProgression = 0.5),
            Locator.Locations.fromJSON("{ \"totalProgression\": 0.5 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(totalProgression = 0.0),
            Locator.Locations.fromJSON("{ \"totalProgression\": 0 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(totalProgression = 1.0),
            Locator.Locations.fromJSON("{ \"totalProgression\": 1 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(),
            Locator.Locations.fromJSON("{ \"totalProgression\": -0.5 }".toJsonObjectOrNull()!!)
        )
        assertEquals(
            Locator.Locations(),
            Locator.Locations.fromJSON("{ \"totalProgression\": 1.2 }".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `get Locations minimal JSON`() {
        assertJSONEquals(
            "{}".toJsonObjectOrNull()!!,
            Locator.Locations().toJSON()
        )
    }

    @Test fun `get Locations full JSON`() {
        assertJSONEquals(
            """{
                "fragments": ["p=4", "frag34"],
                "progression": 0.74,
                "totalProgression": 25.32,
                "position": 42,
                "other": "other-location"
            }""".toJsonObjectOrNull()!!,
            Locator.Locations(
                fragments = listOf("p=4", "frag34"),
                progression = 0.74,
                position = 42,
                totalProgression = 25.32,
                otherLocations = mapOf("other" to "other-location")
            ).toJSON()
        )
    }

    @Test fun `parse Text minimal JSON`() {
        assertEquals(
            Locator.Text(),
            Locator.Text.fromJSON("{}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse Text full JSON`() {
        assertEquals(
            Locator.Text(
                before = "Text before",
                highlight = "Highlighted text",
                after = "Text after"
            ),
            Locator.Text.fromJSON(
                """{
                "before": "Text before",
                "highlight": "Highlighted text",
                "after": "Text after"
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse Text null JSON`() {
        assertEquals(Locator.Text(), Locator.Text.fromJSON(null))
    }

    @Test fun `get Text minimal JSON`() {
        assertJSONEquals(
            "{}".toJsonObjectOrNull()!!,
            Locator.Text().toJSON()
        )
    }

    @Test fun `get Text full JSON`() {
        assertJSONEquals(
            """{
                "before": "Text before",
                "highlight": "Highlighted text",
                "after": "Text after"
            }""".toJsonObjectOrNull()!!,
            Locator.Text(
                before = "Text before",
                highlight = "Highlighted text",
                after = "Text after"
            ).toJSON()
        )
    }

    @Test fun `substring from a range`() {
        val text = Locator.Text(
            before = "before",
            highlight = "highlight",
            after = "after"
        )

        assertEquals(
            Locator.Text(
                before = "before",
                highlight = "h",
                after = "ighlightafter"
            ),
            text.substring(0..-1)
        )

        assertEquals(
            Locator.Text(
                before = "before",
                highlight = "h",
                after = "ighlightafter"
            ),
            text.substring(0..0)
        )

        assertEquals(
            Locator.Text(
                before = "beforehigh",
                highlight = "lig",
                after = "htafter"
            ),
            text.substring(4..6)
        )

        assertEquals(
            Locator.Text(
                before = "before",
                highlight = "highlight",
                after = "after"
            ),
            text.substring(0..8)
        )

        assertEquals(
            Locator.Text(
                before = "beforehighli",
                highlight = "ght",
                after = "after"
            ),
            text.substring(6..12)
        )

        assertEquals(
            Locator.Text(
                before = "beforehighligh",
                highlight = "t",
                after = "after"
            ),
            text.substring(8..12)
        )

        assertEquals(
            Locator.Text(
                before = "beforehighlight",
                highlight = "",
                after = "after"
            ),
            text.substring(9..12)
        )
    }

    @Test fun `substring from a range with null components`() {
        assertEquals(
            Locator.Text(
                before = "high",
                highlight = "lig",
                after = "htafter"
            ),
            Locator.Text(
                before = null,
                highlight = "highlight",
                after = "after"
            ).substring(4..6)
        )

        assertEquals(
            Locator.Text(
                before = "beforehigh",
                highlight = "lig",
                after = "ht"
            ),
            Locator.Text(
                before = "before",
                highlight = "highlight",
                after = null
            ).substring(4..6)
        )

        assertEquals(
            Locator.Text(
                before = "before",
                highlight = null,
                after = "after"
            ),
            Locator.Text(
                before = "before",
                highlight = null,
                after = "after"
            ).substring(4..6)
        )

        assertEquals(
            Locator.Text(
                before = "before",
                highlight = "",
                after = "after"
            ),
            Locator.Text(
                before = "before",
                highlight = "",
                after = "after"
            ).substring(4..6)
        )
    }
}

class LocatorCollectionTest {

    @Test fun `parse LocatorCollection minimal JSON`() {
        assertEquals(
            LocatorCollection(),
            LocatorCollection.fromJSON("{}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse LocatorCollection full JSON`() {
        assertEquals(
            LocatorCollection(
                metadata = LocatorCollection.Metadata(
                    localizedTitle = LocalizedString.fromStrings(
                        mapOf(
                            "en" to "Searching <riddle> in Alice in Wonderlands - Page 1",
                            "fr" to "Recherche <riddle> dans Alice in Wonderlands – Page 1"
                        )
                    ),
                    numberOfItems = 3,
                    otherMetadata = mapOf(
                        "extraMetadata" to "value"
                    )
                ),
                links = listOf(
                    Link(
                        rels = setOf("self"),
                        href = Href("/978-1503222687/search?query=apple")!!,
                        mediaType = MediaType("application/vnd.readium.locators+json")!!
                    ),
                    Link(
                        rels = setOf("next"),
                        href = Href("/978-1503222687/search?query=apple&page=2")!!,
                        mediaType = MediaType("application/vnd.readium.locators+json")!!
                    )
                ),
                locators = listOf(
                    Locator(
                        href = Url("/978-1503222687/chap7.html")!!,
                        mediaType = MediaType.XHTML,
                        locations = Locator.Locations(
                            fragments = listOf(":~:text=riddle,-yet%3F'"),
                            progression = 0.43
                        ),
                        text = Locator.Text(
                            before = "'Have you guessed the ",
                            highlight = "riddle",
                            after = " yet?' the Hatter said, turning to Alice again."
                        )
                    ),
                    Locator(
                        href = Url("/978-1503222687/chap7.html")!!,
                        mediaType = MediaType.XHTML,
                        locations = Locator.Locations(
                            fragments = listOf(":~:text=in%20asking-,riddles"),
                            progression = 0.47
                        ),
                        text = Locator.Text(
                            before = "I'm glad they've begun asking ",
                            highlight = "riddles",
                            after = ".--I believe I can guess that,"
                        )
                    )
                )
            ),
            LocatorCollection.fromJSON(
                """{
              "metadata": {
                "title": {
                    "en": "Searching <riddle> in Alice in Wonderlands - Page 1",
                    "fr": "Recherche <riddle> dans Alice in Wonderlands – Page 1"
                },
                "numberOfItems": 3,
                "extraMetadata": "value"
              },
              "links": [
                {"rel": "self", "href": "/978-1503222687/search?query=apple", "type": "application/vnd.readium.locators+json"},
                {"rel": "next", "href": "/978-1503222687/search?query=apple&page=2", "type": "application/vnd.readium.locators+json"}
              ],
              "locators": [
                {
                  "href": "/978-1503222687/chap7.html",
                  "type": "application/xhtml+xml",
                  "locations": {
                    "fragments": [
                      ":~:text=riddle,-yet%3F'"
                    ],
                    "progression": 0.43
                  },
                  "text": {
                    "before": "'Have you guessed the ",
                    "highlight": "riddle",
                    "after": " yet?' the Hatter said, turning to Alice again."
                  }
                },
                {
                  "href": "/978-1503222687/chap7.html",
                  "type": "application/xhtml+xml",
                  "locations": {
                    "fragments": [
                      ":~:text=in%20asking-,riddles"
                    ],
                    "progression": 0.47
                  },
                  "text": {
                    "before": "I'm glad they've begun asking ",
                    "highlight": "riddles",
                    "after": ".--I believe I can guess that,"
                  }
                }
              ]
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `get Locator minimal JSON`() {
        assertJSONEquals(
            """{
                "locators": []
            }""".toJsonObjectOrNull()!!,
            LocatorCollection().toJSON()
        )
    }

    @Test fun `get Locator full JSON`() {
        assertJSONEquals(
            """{
              "metadata": {
                "title": {
                    "en": "Searching <riddle> in Alice in Wonderlands - Page 1",
                    "fr": "Recherche <riddle> dans Alice in Wonderlands – Page 1"
                },
                "numberOfItems": 3,
                "extraMetadata": "value"
              },
              "links": [
                {"rel": ["self"], "href": "/978-1503222687/search?query=apple", "type": "application/vnd.readium.locators+json", "templated": false},
                {"rel": ["next"], "href": "/978-1503222687/search?query=apple&page=2", "type": "application/vnd.readium.locators+json", "templated": false}
              ],
              "locators": [
                {
                  "href": "/978-1503222687/chap7.html",
                  "type": "application/xhtml+xml",
                  "locations": {
                    "fragments": [
                      ":~:text=riddle,-yet%3F'"
                    ],
                    "progression": 0.43
                  },
                  "text": {
                    "before": "'Have you guessed the ",
                    "highlight": "riddle",
                    "after": " yet?' the Hatter said, turning to Alice again."
                  }
                },
                {
                  "href": "/978-1503222687/chap7.html",
                  "type": "application/xhtml+xml",
                  "locations": {
                    "fragments": [
                      ":~:text=in%20asking-,riddles"
                    ],
                    "progression": 0.47
                  },
                  "text": {
                    "before": "I'm glad they've begun asking ",
                    "highlight": "riddles",
                    "after": ".--I believe I can guess that,"
                  }
                }
              ]
            }""".toJsonObjectOrNull()!!,
            LocatorCollection(
                metadata = LocatorCollection.Metadata(
                    localizedTitle = LocalizedString.fromStrings(
                        mapOf(
                            "en" to "Searching <riddle> in Alice in Wonderlands - Page 1",
                            "fr" to "Recherche <riddle> dans Alice in Wonderlands – Page 1"
                        )
                    ),
                    numberOfItems = 3,
                    otherMetadata = mapOf(
                        "extraMetadata" to "value"
                    )
                ),
                links = listOf(
                    Link(
                        rels = setOf("self"),
                        href = Href("/978-1503222687/search?query=apple")!!,
                        mediaType = MediaType("application/vnd.readium.locators+json")!!
                    ),
                    Link(
                        rels = setOf("next"),
                        href = Href("/978-1503222687/search?query=apple&page=2")!!,
                        mediaType = MediaType("application/vnd.readium.locators+json")!!
                    )
                ),
                locators = listOf(
                    Locator(
                        href = Url("/978-1503222687/chap7.html")!!,
                        mediaType = MediaType.XHTML,
                        locations = Locator.Locations(
                            fragments = listOf(":~:text=riddle,-yet%3F'"),
                            progression = 0.43
                        ),
                        text = Locator.Text(
                            before = "'Have you guessed the ",
                            highlight = "riddle",
                            after = " yet?' the Hatter said, turning to Alice again."
                        )
                    ),
                    Locator(
                        href = Url("/978-1503222687/chap7.html")!!,
                        mediaType = MediaType.XHTML,
                        locations = Locator.Locations(
                            fragments = listOf(":~:text=in%20asking-,riddles"),
                            progression = 0.47
                        ),
                        text = Locator.Text(
                            before = "I'm glad they've begun asking ",
                            highlight = "riddles",
                            after = ".--I believe I can guess that,"
                        )
                    )
                )
            ).toJSON()
        )
    }
}
