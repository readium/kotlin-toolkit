/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.extensions

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.publication.link.Properties

class EncryptionTest {

    @Test
    fun `parse minimal JSON`() {
        assertEquals(
            Encryption(algorithm = "http://algo"),
            Encryption.fromJSON(JSONObject("{'algorithm': 'http://algo'}"))
        )
    }

    @Test
    fun `parse full JSON`() {
        assertEquals(
            Encryption(
                algorithm = "http://algo",
                compression = "gzip",
                originalLength = 42099,
                profile = "http://profile",
                scheme = "http://scheme"
            ),
            Encryption.fromJSON(JSONObject("""{
                "algorithm": "http://algo",
                "compression": "gzip",
                "originalLength": 42099,
                "profile": "http://profile",
                "scheme": "http://scheme"
            }"""))
        )
    }

    @Test
    fun `parse null JSON`() {
        assertNull(Encryption.fromJSON(null))
    }

    @Test
    fun `parse JSON requires {algorithm}`() {
        assertNull(Encryption.fromJSON(JSONObject("{'compression': 'gzip'}")))
    }

    @Test
    fun `get minimal JSON`() {
        assertJSONEquals(
            JSONObject("{'algorithm': 'http://algo'}"),
            Encryption(algorithm = "http://algo").toJSON()
        )
    }

    @Test
    fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "algorithm": "http://algo",
                "compression": "gzip",
                "originalLength": 42099,
                "profile": "http://profile",
                "scheme": "http://scheme"
            }"""),
            Encryption(
                algorithm = "http://algo",
                compression = "gzip",
                originalLength = 42099,
                profile = "http://profile",
                scheme = "http://scheme"
            ).toJSON()
        )
    }


    // Encryption extensions for link [Properties].

    @Test fun `get Properties {encryption} when available`() {
        assertEquals(
            Encryption(algorithm = "http://algo", compression = "gzip"),
            Properties(otherProperties = mapOf("encrypted" to mapOf(
                "algorithm" to "http://algo",
                "compression" to "gzip"
            ))).encryption
        )
    }

    @Test fun `get Properties {encryption} when missing`() {
        assertNull(Properties().encryption)
    }

    @Test fun `get Properties {encryption} when not valid`() {
        assertNull(Properties(otherProperties = mapOf("encrypted" to "invalid")).encryption)
    }

}