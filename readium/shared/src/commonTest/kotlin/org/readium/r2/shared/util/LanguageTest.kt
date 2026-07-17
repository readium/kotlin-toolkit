/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageTest {

    @Test
    fun normalizesUnderscoresToHyphens() {
        assertEquals("en-US", Language("en_US").code)
        assertEquals("en-US", Language("en-US").code)
    }

    @Test
    fun isRegional() {
        assertFalse(Language("en").isRegional)
        assertTrue(Language("en-US").isRegional)
        assertTrue(Language("en_US").isRegional)
    }

    @Test
    fun removeRegion() {
        assertEquals(Language("en"), Language("en").removeRegion())
        assertEquals(Language("en"), Language("en-US").removeRegion())
    }

    @Test
    fun equalityIsBasedOnCode() {
        assertEquals(Language("en-US"), Language("en_US"))
        assertEquals(Language("en-US").hashCode(), Language("en_US").hashCode())
    }
}
