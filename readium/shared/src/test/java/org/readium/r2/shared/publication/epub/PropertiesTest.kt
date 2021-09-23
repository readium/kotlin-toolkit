/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.Properties

class PropertiesTest {

    @Test fun `get Properties {contains} when available`() {
        assertEquals(
            setOf("mathml", "onix"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "onix"))).contains
        )
    }

    @Test fun `get Properties {contains} removes duplicates`() {
        assertEquals(
            setOf("mathml", "onix"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "onix", "onix"))).contains
        )
    }

    @Test fun `get Properties {contains} when missing`() {
        assertEquals(emptySet<String>(), Properties().contains)
    }

    @Test fun `get Properties {contains} skips duplicates`() {
        assertEquals(
            setOf("mathml"),
            Properties(otherProperties = mapOf("contains" to listOf("mathml", "mathml"))).contains
        )
    }

    @Test fun `get Properties {layout} when available`() {
        assertEquals(
            EpubLayout.FIXED,
            Properties(otherProperties = mapOf("layout" to "fixed")).layout
        )
    }

    @Test fun `get Properties {layout} when missing`() {
        assertNull(Properties().layout)
    }

}
