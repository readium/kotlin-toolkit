/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.encryption

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.Properties

class PropertiesTest {

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