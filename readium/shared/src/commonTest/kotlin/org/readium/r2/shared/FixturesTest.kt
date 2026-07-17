/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class FixturesTest {

    @Test
    fun readsATextFixture() {
        val content = Fixtures("kmp").read("hello.txt").utf8()
        assertEquals("Hello, Readium!", content.trim())
    }
}
