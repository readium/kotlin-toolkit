/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu, Quentin Gliosca
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

class LinkTest {

    @Test fun `templateParameters works fine`() {
        val href =  "/url{?x,hello,y}name{z,y,w}"
        assertEquals(
            listOf("x", "hello", "y", "z", "w"),
            Link(href = href, templated = true).templateParameters
        )
    }

    @Test fun `expand works fine with simple string templates`() {
        val href =  "/url{x,hello,y}name{z,y,w}"
        val parameters = mapOf(
            "x" to "aaa",
            "hello" to "Hello, world",
            "y" to "b",
            "z" to "45",
            "w" to "w"
        )
        assertEquals(
            "/urlaaa,Hello, world,bname45,b,w",
            Link(href = href, templated = true).expand(parameters)
        )
    }

    @Test fun `expand works fine with form-style ampersand-separated templates`() {
        val href =  "/url{?x,hello,y}name"
        val parameters = mapOf(
            "x" to "aaa",
            "hello" to "Hello, world",
            "y" to "b"
        )
        assertEquals(
            "/url?x=aaa&hello=Hello, world&y=bname",
            Link(href = href, templated = true).expand(parameters)
        )
    }

    @Test fun `expand adds extra parameters as query parameters`() {
        assertEquals(
            "/path?search=banana&code=14",
            Link(href = "/path{?search}", templated = true).expand(
                mapOf(
                    "search" to "banana",
                    "code" to "14"
                )
            )
        )
    }

    @Test fun `expand adds a query for extra parameters`() {
        assertEquals(
            "/path?search=banana&code=14",
            Link(href = "/path", templated = true).expand(
                mapOf(
                    "search" to "banana",
                    "code" to "14"
                )
            )
        )
    }

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

    @Test
    fun `Make a copy after adding the given {properties}`() {
        val link = Link(
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
        )

        assertJSONEquals(
            JSONObject("""{
                "href": "http://href",
                "type": "application/pdf",
                "templated": true,
                "title": "Link Title",
                "rel": ["publication", "cover"],
                "properties": {
                    "orientation": "landscape",
                    "additional": "property"
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
            link.addProperties(mapOf("additional" to "property")).toJSON()
        )
    }

    @Test
    fun `Find the first index of the {Link} with the given {href} in a list of {Link}`() {
        assertNull(listOf(Link(href = "href")).indexOfFirstWithHref("foobar"))

        assertEquals(
            1,
            listOf(
                Link(href = "href1"),
                Link(href = "href2"),
                Link(href = "href2")  // duplicated on purpose
            ).indexOfFirstWithHref("href2")
        )
    }

}
