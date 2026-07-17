/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication.encryption

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.json.toJsonObjectOrNull
import org.readium.r2.shared.util.json.toMap

class EncryptionTest {

    @Test
    fun `parse minimal JSON`() {
        assertEquals(
            Encryption(algorithm = "http://algo"),
            Encryption.fromJSON("{\"algorithm\": \"http://algo\"}".toJsonObjectOrNull()!!)
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
            Encryption.fromJSON(
                """{
                "algorithm": "http://algo",
                "compression": "gzip",
                "originalLength": 42099,
                "profile": "http://profile",
                "scheme": "http://scheme"
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test
    fun `parse null JSON`() {
        assertNull(Encryption.fromJSON(null))
    }

    @Test
    fun `parse JSON requires algorithm`() {
        assertNull(Encryption.fromJSON("{\"compression\": \"gzip\"}".toJsonObjectOrNull()!!))
    }

    @Test
    fun `get minimal JSON`() {
        assertJSONEquals(
            "{\"algorithm\": \"http://algo\"}".toJsonObjectOrNull()!!,
            Encryption(algorithm = "http://algo").toJSON()
        )
    }

    @Test
    fun `get full JSON`() {
        // We build directly the map because the JSON unwrapping uses [Int] for numbers fitting
        // in an [Int], which doesn't match [originalLength].
        assertEquals(
            mapOf(
                "algorithm" to "http://algo",
                "compression" to "gzip",
                "originalLength" to 42099,
                "profile" to "http://profile",
                "scheme" to "http://scheme"
            ),
            Encryption(
                algorithm = "http://algo",
                compression = "gzip",
                originalLength = 42099,
                profile = "http://profile",
                scheme = "http://scheme"
            ).toJSON().toMap()
        )
    }
}
