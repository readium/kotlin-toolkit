/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ClockValueParserTest {
    private val parser = ClockValueParser

    @Test
    fun `Full and partial clock values are rightly parsed`() {
        assertThat(parser.parse("02:30:03")).isEqualTo(9003.0)
        assertThat(parser.parse("50:00:10.25")).isEqualTo(180010.25)
        assertThat(parser.parse(" 02:33")).isEqualTo(153.0)
        assertThat(parser.parse("00:10.5")).isEqualTo(10.5)
    }

    @Test
    fun `Timecounts are rightly parsed`() {
        assertThat(parser.parse("3.2h")).isEqualTo(11520.0)
        assertThat(parser.parse("45min")).isEqualTo(2700.0)
        assertThat(parser.parse(" 30s")).isEqualTo(30.0)
        assertThat(parser.parse("5ms")).isEqualTo(0.005)
        assertThat(parser.parse("12.467")).isEqualTo(12.467)
    }
}