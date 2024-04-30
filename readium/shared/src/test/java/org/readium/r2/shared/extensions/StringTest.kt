/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.extensions

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class StringTest {

    @Test
    fun `checks if a string is made of printable ASCII characters`() {
        assertTrue("".isPrintableAscii())
        assertTrue(" foo/@bar".isPrintableAscii())
        assertFalse("école".isPrintableAscii())
        assertFalse("\u0001 non printable".isPrintableAscii())
    }
}
