/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub.link

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PropertiesTest {

    @Test
    fun `parse orientation`() {
        assertEquals(Properties.Orientation.AUTO, Properties.Orientation.from("auto"))
        assertEquals(Properties.Orientation.LANDSCAPE, Properties.Orientation.from("landscape"))
        assertEquals(Properties.Orientation.PORTRAIT, Properties.Orientation.from("portrait"))
        assertNull(Properties.Orientation.from("foobar"))
        assertNull(Properties.Orientation.from(null))
    }

    @Test
    fun `parse page`() {
        assertEquals(Properties.Page.LEFT, Properties.Page.from("left"))
        assertEquals(Properties.Page.RIGHT, Properties.Page.from("right"))
        assertEquals(Properties.Page.CENTER, Properties.Page.from("center"))
        assertNull(Properties.Page.from("foobar"))
        assertNull(Properties.Page.from(null))
    }

    @Test
    fun `parse null JSON`() {
        assertEquals(Properties(), Properties.fromJSON(null))
    }

    @Test
    fun `parse minimal JSON`() {
        assertEquals(Properties(), Properties.fromJSON(JSONObject("{}")))
    }

    @Test
    fun `parse full JSON`() {
        assertEquals(
            Properties(
                orientation = Properties.Orientation.AUTO,
                page = Properties.Page.LEFT,
                otherProperties = mapOf<String, Any>(
                    "other-property1" to "value",
                    "other-property2" to listOf(42)
                )
            ),
            Properties.fromJSON(JSONObject("""{
                "orientation": "auto",
                "page": "left",
                "other-property1": "value",
                "other-property2": [42]
            }"""))
        )
    }

    @Test
    fun `get minimal JSON`() {
        assertEquals("{}", Properties().toJSON().toString())
    }

    @Test
    fun `get full JSON`() {
        assertEquals(
            JSONObject("""{
                "orientation": "landscape",
                "page": "right",
                "other-property1": "value",
                "other-property2": [42]
            }""").toString(),
            Properties(
                orientation = Properties.Orientation.LANDSCAPE,
                page = Properties.Page.RIGHT,
                otherProperties = mapOf<String, Any>(
                    "other-property1" to "value",
                    "other-property2" to listOf(42)
                )
            ).toJSON().toString()
        )
    }

}