/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.extensions

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class StringTest {

    @Test
    fun `converts a ISO-8601 string to a Date`() {
        assertNull("invalid".iso8601ToDate())

        assertEquals(1712707200000, "2024-04-10".iso8601ToDate()?.time)
        assertEquals(1712746680000, "2024-04-10T10:58".iso8601ToDate()?.time)
        assertEquals(1712746724000, "2024-04-10T10:58:44".iso8601ToDate()?.time)
        assertEquals(1712746724000, "2024-04-10T10:58:44Z".iso8601ToDate()?.time)
        assertEquals(1712746724000, "2024-04-10T10:58:44.000Z".iso8601ToDate()?.time)
    }

    @Test
    fun `checks if a string is made of printable ASCII characters`() {
        assertTrue("".isPrintableAscii())
        assertTrue(" foo/@bar".isPrintableAscii())
        assertFalse("école".isPrintableAscii())
        assertFalse("\u0001 non printable".isPrintableAscii())
    }
}
