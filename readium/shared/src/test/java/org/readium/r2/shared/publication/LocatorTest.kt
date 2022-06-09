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
import org.junit.runner.RunWith
import org.readium.r2.shared.assertJSONEquals
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocatorTest {

    @Test fun `parse {Locator} minimal JSON`() {
        assertEquals(
            Locator(href = "http://locator", type = "text/html"),
            Locator.fromJSON(JSONObject("""{
                "href": "http://locator",
                "type": "text/html"
            }"""))
        )
    }

    @Test fun `parse {Locator} full JSON`() {
        assertEquals(
            Locator(
                href = "http://locator",
                type = "text/html",
                title = "My Locator",
                locations = Locator.Locations(position = 42),
                text = Locator.Text(highlight = "Excerpt")
            ),
            Locator.fromJSON(JSONObject("""{
                "href": "http://locator",
                "type": "text/html",
                "title": "My Locator",
                "locations": {
                    "position": 42
                },
                "text": {
                    "highlight": "Excerpt"
                }
            }"""))
        )
    }

    @Test fun `parse {Locator} null JSON`() {
        assertEquals(null, Locator.fromJSON(null))
    }

    @Test fun `parse {Locator} invalid JSON`() {
        assertNull(Locator.fromJSON(JSONObject("{ 'invalid': 'object' }")))
    }

    @Test fun `get {Locator} minimal JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "href": "http://locator",
                "type": "text/html"
            }"""),
            Locator(href = "http://locator", type = "text/html").toJSON()
        )
    }

    @Test fun `get {Locator} full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "href": "http://locator",
                "type": "text/html",
                "title": "My Locator",
                "locations": {
                    "position": 42
                },
                "text": {
                    "highlight": "Excerpt"
                }
            }"""),
            Locator(
                href = "http://locator",
                type = "text/html",
                title = "My Locator",
                locations = Locator.Locations(position = 42),
                text = Locator.Text(highlight = "Excerpt")
            ).toJSON()
        )
    }

    @Test fun `copy a {Locator} with different {Locations} sub-properties`() {
        assertEquals(
            Locator(
                href = "http://locator",
                type = "text/html",
                locations = Locator.Locations(
                    fragments = listOf("p=4", "frag34"),
                    progression = 0.74,
                    position = 42,
                    totalProgression = 0.32,
                    otherLocations = mapOf("other" to "other-location")
                )
            ),
            Locator(
                href = "http://locator",
                type = "text/html",
                locations = Locator.Locations(position = 42, progression = 2.0)
            ).copyWithLocations(
                fragments = listOf("p=4", "frag34"),
                progression = 0.74,
                totalProgression = 0.32,
                otherLocations = mapOf("other" to "other-location")
            )
        )
    }

    @Test fun `copy a {Locator} with reset {Locations} sub-properties`() {
        assertEquals(
            Locator(
                href = "http://locator",
                type = "text/html",
                locations = Locator.Locations()
            ),
            Locator(
                href = "http://locator",
                type = "text/html",
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

    @Test fun `parse {Locations} minimal JSON`() {
        assertEquals(
            Locator.Locations(),
            Locator.Locations.fromJSON(JSONObject("{}"))
        )
    }

    @Test fun `parse {Locations} full JSON`() {
        assertEquals(
            Locator.Locations(
                fragments = listOf("p=4", "frag34"),
                progression = 0.74,
                position = 42,
                totalProgression = 0.32,
                otherLocations = mapOf("other" to "other-location")
            ),
            Locator.Locations.fromJSON(JSONObject("""{
                "fragments": ["p=4", "frag34"],
                "progression": 0.74,
                "totalProgression": 0.32,
                "position": 42,
                "other": "other-location"
            }"""))
        )
    }

    @Test fun `parse {Locations} null JSON`() {
        assertEquals(Locator.Locations(), Locator.Locations.fromJSON(null))
    }

    @Test fun `parse {Locations} single fragment JSON`() {
        assertEquals(
            Locator.Locations(fragments = listOf("frag34")),
            Locator.Locations.fromJSON(JSONObject("{ 'fragment': 'frag34' }"))
        )
    }

    @Test fun `parse {Locations} ignores {position} smaller than 1`() {
        assertEquals(Locator.Locations(position = 1), Locator.Locations.fromJSON(JSONObject("{ 'position': 1 }")))
        assertEquals(Locator.Locations(), Locator.Locations.fromJSON(JSONObject("{ 'position': 0 }")))
        assertEquals(Locator.Locations(), Locator.Locations.fromJSON(JSONObject("{ 'position': -1 }")))
    }

    @Test fun `parse {Locations} ignores {progression} outside of 0-1 range`() {
        assertEquals(Locator.Locations(progression = 0.5), Locator.Locations.fromJSON(JSONObject("{ 'progression': 0.5 }")))
        assertEquals(Locator.Locations(progression = 0.0), Locator.Locations.fromJSON(JSONObject("{ 'progression': 0 }")))
        assertEquals(Locator.Locations(progression = 1.0), Locator.Locations.fromJSON(JSONObject("{ 'progression': 1 }")))
        assertEquals(Locator.Locations(), Locator.Locations.fromJSON(JSONObject("{ 'progression': -0.5 }")))
        assertEquals(Locator.Locations(), Locator.Locations.fromJSON(JSONObject("{ 'progression': 1.2 }")))
    }

    @Test fun `parse {Locations} ignores {totalProgression} outside of 0-1 range`() {
        assertEquals(Locator.Locations(totalProgression = 0.5), Locator.Locations.fromJSON(JSONObject("{ 'totalProgression': 0.5 }")))
        assertEquals(Locator.Locations(totalProgression = 0.0), Locator.Locations.fromJSON(JSONObject("{ 'totalProgression': 0 }")))
        assertEquals(Locator.Locations(totalProgression = 1.0), Locator.Locations.fromJSON(JSONObject("{ 'totalProgression': 1 }")))
        assertEquals(Locator.Locations(), Locator.Locations.fromJSON(JSONObject("{ 'totalProgression': -0.5 }")))
        assertEquals(Locator.Locations(), Locator.Locations.fromJSON(JSONObject("{ 'totalProgression': 1.2 }")))
    }

    @Test fun `get {Locations} minimal JSON`() {
        assertJSONEquals(
            JSONObject("{}"),
            Locator.Locations().toJSON()
        )
    }

    @Test fun `get {Locations} full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "fragments": ["p=4", "frag34"],
                "progression": 0.74,
                "totalProgression": 25.32,
                "position": 42,
                "other": "other-location"
            }"""),
            Locator.Locations(
                fragments = listOf("p=4", "frag34"),
                progression = 0.74,
                position = 42,
                totalProgression = 25.32,
                otherLocations = mapOf("other" to "other-location")
            ).toJSON()
        )
    }

    @Test fun `parse {Text} minimal JSON`() {
        assertEquals(
            Locator.Text(),
            Locator.Text.fromJSON(JSONObject("{}"))
        )
    }

    @Test fun `parse {Text} full JSON`() {
        assertEquals(
            Locator.Text(
                before = "Text before",
                highlight = "Highlighted text",
                after = "Text after"
            ),
            Locator.Text.fromJSON(JSONObject("""{
                "before": "Text before",
                "highlight": "Highlighted text",
                "after": "Text after"
            }"""))
        )
    }

    @Test fun `parse {Text} null JSON`() {
        assertEquals(Locator.Text(), Locator.Text.fromJSON(null))
    }

    @Test fun `get {Text} minimal JSON`() {
        assertJSONEquals(
            JSONObject("{}"),
            Locator.Text().toJSON()
        )
    }

    @Test fun `get {Text} full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "before": "Text before",
                "highlight": "Highlighted text",
                "after": "Text after"
            }"""),
            Locator.Text(
                before = "Text before",
                highlight = "Highlighted text",
                after = "Text after"
            ).toJSON()
        )
    }

}

@RunWith(RobolectricTestRunner::class)
class LocatorCollectionTest {

    @Test fun `parse {LocatorCollection} minimal JSON`() {
        assertEquals(
            LocatorCollection(),
            LocatorCollection.fromJSON(JSONObject("{}"))
        )
    }

    @Test fun `parse {LocatorCollection} full JSON`() {
        assertEquals(
            LocatorCollection(
                metadata = LocatorCollection.Metadata(
                    localizedTitle = LocalizedString.fromStrings(mapOf(
                        "en" to "Searching <riddle> in Alice in Wonderlands - Page 1",
                        "fr" to "Recherche <riddle> dans Alice in Wonderlands – Page 1"
                    )),
                    numberOfItems = 3,
                    otherMetadata = mapOf(
                        "extraMetadata" to "value"
                    )
                ),
                links = listOf(
                    Link(rels = setOf("self"), href = "/978-1503222687/search?query=apple", type = "application/vnd.readium.locators+json"),
                    Link(rels = setOf("next"), href = "/978-1503222687/search?query=apple&page=2", type = "application/vnd.readium.locators+json"),
                ),
                locators = listOf(
                    Locator(
                        href = "/978-1503222687/chap7.html",
                        type = "application/xhtml+xml",
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
                        href = "/978-1503222687/chap7.html",
                        type = "application/xhtml+xml",
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
            LocatorCollection.fromJSON(JSONObject("""{
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
            }"""))
        )
    }

    @Test fun `get {Locator} minimal JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "locators": []
            }"""),
            LocatorCollection().toJSON()
        )
    }

    @Test fun `get {Locator} full JSON`() {
        assertJSONEquals(
            JSONObject("""{
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
            }"""),
            LocatorCollection(
                metadata = LocatorCollection.Metadata(
                    localizedTitle = LocalizedString.fromStrings(mapOf(
                        "en" to "Searching <riddle> in Alice in Wonderlands - Page 1",
                        "fr" to "Recherche <riddle> dans Alice in Wonderlands – Page 1"
                    )),
                    numberOfItems = 3,
                    otherMetadata = mapOf(
                        "extraMetadata" to "value"
                    )
                ),
                links = listOf(
                    Link(rels = setOf("self"), href = "/978-1503222687/search?query=apple", type = "application/vnd.readium.locators+json"),
                    Link(rels = setOf("next"), href = "/978-1503222687/search?query=apple&page=2", type = "application/vnd.readium.locators+json"),
                ),
                locators = listOf(
                    Locator(
                        href = "/978-1503222687/chap7.html",
                        type = "application/xhtml+xml",
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
                        href = "/978-1503222687/chap7.html",
                        type = "application/xhtml+xml",
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
