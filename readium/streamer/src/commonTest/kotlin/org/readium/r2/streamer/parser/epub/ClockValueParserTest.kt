/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import kotlin.test.Test
import kotlin.test.assertEquals

class ClockValueParserTest {
    private val parser = ClockValueParser

    @Test
    fun `Full and partial clock values are rightly parsed`() {
        assertEquals(9003.0, parser.parse("02:30:03"))
        assertEquals(180010.25, parser.parse("50:00:10.25"))
        assertEquals(153.0, parser.parse(" 02:33"))
        assertEquals(10.5, parser.parse("00:10.5"))
    }

    @Test
    fun `Timecounts are rightly parsed`() {
        assertEquals(11520.0, parser.parse("3.2h"))
        assertEquals(2700.0, parser.parse("45min"))
        assertEquals(30.0, parser.parse(" 30s"))
        assertEquals(0.005, parser.parse("5ms"))
        assertEquals(12.467, parser.parse("12.467"))
    }
}
