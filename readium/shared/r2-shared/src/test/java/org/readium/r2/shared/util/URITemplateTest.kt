/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util

import org.junit.Test
import kotlin.test.assertEquals

class URITemplateTest {

    @Test
    fun `parameters works fine`() {
        assertEquals(
            listOf("x", "hello", "y", "z", "w"),
            URITemplate("/url{?x,hello,y}name{z,y,w}").parameters
        )
    }

    @Test
    fun `expand works fine with simple string templates`() {
        val template =  URITemplate("/url{x,hello,y}name{z,y,w}")
        val parameters = mapOf(
            "x" to "aaa",
            "hello" to "Hello, world",
            "y" to "b",
            "z" to "45",
            "w" to "w"
        )
        assertEquals(
            "/urlaaa,Hello, world,bname45,b,w",
            template.expand(parameters)
        )
    }

    @Test
    fun `expand works fine with form-style ampersand-separated templates`() {
        val template =  URITemplate("/url{?x,hello,y}name")
        val parameters = mapOf(
            "x" to "aaa",
            "hello" to "Hello, world",
            "y" to "b"
        )
        assertEquals(
            "/url?x=aaa&hello=Hello, world&y=bname",
            template.expand(parameters)
        )
    }
}