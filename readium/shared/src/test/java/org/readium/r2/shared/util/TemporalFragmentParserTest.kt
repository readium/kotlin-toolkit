/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class TemporalFragmentParserTest {

    @Test
    fun `fragment which is not temporal is rejected`() {
        assertNull(TemporalFragmentParser.parse("htmlId"))
    }

    @Test
    fun `start only in seconds is accepted`() {
        assertEquals(
            TimeInterval(start = 4.seconds, null),
            TemporalFragmentParser.parse("t=4")
        )
    }

    @Test
    fun `end only in seconds is accepted`() {
        assertEquals(
            TimeInterval(start = null, end = 40.seconds),
            TemporalFragmentParser.parse("t=,40")
        )
    }

    @Test
    fun `start and end in seconds are accepted`() {
        assertEquals(
            TimeInterval(4.seconds, 60.seconds),
            TemporalFragmentParser.parse("t=4,60")
        )
    }

    @Test
    fun `npt prefix is accepted`() {
        assertEquals(
            TimeInterval(40.seconds, null),
            TemporalFragmentParser.parse("t=npt:40")
        )
    }

    @Test
    fun `floating point values are accepted`() {
        assertEquals(
            TimeInterval(40.5.seconds, 83.235.seconds),
            TemporalFragmentParser.parse("t=40.500,83.235")
        )
    }
}
