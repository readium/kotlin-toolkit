package org.readium.r2.shared.publication

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.toJSON

class LinkTest {

    @Test fun `parse minimal JSON`() {
        assertEquals(
            Link(href = "http://href"),
            Link.fromJSON(JSONObject("{'href': 'http://href'}"))
        )
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            Link(
                href = "http://href",
                type = "application/pdf",
                templated = true,
                title = "Link Title",
                rels = setOf("publication", "cover"),
                properties = Properties(otherProperties = mapOf("orientation" to "landscape")),
                height = 1024,
                width = 768,
                bitrate = 74.2,
                duration = 45.6,
                languages = listOf("fr"),
                alternates = listOf(
                    Link(href = "/alternate1"),
                    Link(href = "/alternate2")
                ),
                children = listOf(
                    Link(href = "http://child1"),
                    Link(href = "http://child2")
                )
            ),
            Link.fromJSON(JSONObject("""{
                "href": "http://href",
                "type": "application/pdf",
                "templated": true,
                "title": "Link Title",
                "rel": ["publication", "cover"],
                "properties": {
                    "orientation": "landscape"
                },
                "height": 1024,
                "width": 768,
                "bitrate": 74.2,
                "duration": 45.6,
                "language": "fr",
                "alternate": [
                    {"href": "/alternate1"},
                    {"href": "/alternate2"}
                ],
                "children": [
                    {"href": "http://child1"},
                    {"href": "http://child2"}
                ]
            }"""))
        )
    }

    @Test fun `parse null JSON`() {
        assertNull(Link.fromJSON(null))
    }

    @Test fun `parse JSON {rel} as single string`() {
        assertEquals(
            Link.fromJSON(JSONObject("{'href': 'a', 'rel': 'publication'}")),
            Link(href = "a", rels = setOf("publication"))
        )
    }

    @Test fun `parse JSON {templated} defaults to false`() {
        val link = Link.fromJSON(JSONObject("{'href': 'a'}"))!!
        assertFalse(link.templated)
    }

    @Test fun `parse JSON {templated} as false when null`() {
        val link = Link.fromJSON(JSONObject("{'href': 'a', 'templated': null}"))!!
        assertFalse(link.templated)
    }

    @Test fun `parse JSON multiple languages`() {
        assertEquals(
            Link.fromJSON(JSONObject("{'href': 'a', 'language': ['fr', 'en']}")),
            Link(href = "a", languages = listOf("fr", "en"))
        )
    }

    @Test fun `parse JSON requires href`() {
        assertNull(Link.fromJSON(JSONObject("{'type': 'application/pdf'}")))
    }

    @Test fun `parse JSON requires positive width`() {
        val link = Link.fromJSON(JSONObject("{'href': 'a', 'width': -20}"))!!
        assertNull(link.width)
    }

    @Test fun `parse JSON requires positive height`() {
        val link = Link.fromJSON(JSONObject("{'href': 'a', 'height': -20}"))!!
        assertNull(link.height)
    }

    @Test fun `parse JSON requires positive bitrate`() {
        val link = Link.fromJSON(JSONObject("{'href': 'a', 'bitrate': -20}"))!!
        assertNull(link.bitrate)
    }

    @Test fun `parse JSON requires positive duration`() {
        val link = Link.fromJSON(JSONObject("{'href': 'a', 'duration': -20}"))!!
        assertNull(link.duration)
    }

    @Test fun `parse JSON array`() {
        assertEquals(
            listOf(
                Link(href = "http://child1"),
                Link(href = "http://child2")
            ),
            Link.fromJSONArray(JSONArray("""[
                {'href': 'http://child1'},
                {'href': 'http://child2'},
            ]"""))
        )
    }

    @Test fun `parse null JSON array`() {
        assertEquals(emptyList<Link>(), Link.fromJSONArray(null))
    }

    @Test fun `parse JSON array ignores invalid links`() {
        assertEquals(
            listOf(
                Link(href = "http://child2")
            ),
            Link.fromJSONArray(JSONArray("""[
                {'title': 'Title'},
                {'href': 'http://child2'},
            ]"""))
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("{'href': 'http://href', 'templated': false}"),
            Link(href = "http://href").toJSON()
        )
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "href": "http://href",
                "type": "application/pdf",
                "templated": true,
                "title": "Link Title",
                "rel": ["publication", "cover"],
                "properties": {
                    "orientation": "landscape"
                },
                "height": 1024,
                "width": 768,
                "bitrate": 74.2,
                "duration": 45.6,
                "language": ["fr"],
                "alternate": [
                    {"href": "/alternate1", "templated": false},
                    {"href": "/alternate2", "templated": false}
                ],
                "children": [
                    {"href": "http://child1", "templated": false},
                    {"href": "http://child2", "templated": false}
                ]
            }"""),
            Link(
                href = "http://href",
                type = "application/pdf",
                templated = true,
                title = "Link Title",
                rels = setOf("publication", "cover"),
                properties = Properties(otherProperties = mapOf("orientation" to "landscape")),
                height = 1024,
                width = 768,
                bitrate = 74.2,
                duration = 45.6,
                languages = listOf("fr"),
                alternates = listOf(
                    Link(href = "/alternate1"),
                    Link(href = "/alternate2")
                ),
                children = listOf(
                    Link(href = "http://child1"),
                    Link(href = "http://child2")
                )
            ).toJSON()
        )
    }

    @Test fun `get JSON array`() {
        assertJSONEquals(
            JSONArray("""[
                {'href': 'http://child1', 'templated': false},
                {'href': 'http://child2', 'templated': false},
            ]"""),
            listOf(
                Link(href = "http://child1"),
                Link(href = "http://child2")
            ).toJSON()
        )
    }

}
