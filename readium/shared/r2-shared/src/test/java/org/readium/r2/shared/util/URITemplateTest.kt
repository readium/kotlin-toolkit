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
            "/urlaaa,Hello,%20world,bname45,b,w",
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
            "/url?x=aaa&hello=Hello,%20world&y=bname",
            template.expand(parameters)
        )

        assertEquals(
            "https://lsd-test.edrlab.org/licenses/39ef1ff2-cda2-4219-a26a-d504fbb24c17/renew?end=2020-11-12T16:02:00.000%2B01:00&id=38dfd7ba-a80b-4253-a047-e6aa9c21d6f0&name=Pixel%203a",
            URITemplate("https://lsd-test.edrlab.org/licenses/39ef1ff2-cda2-4219-a26a-d504fbb24c17/renew{?end,id,name}")
                .expand(mapOf(
                    "id" to "38dfd7ba-a80b-4253-a047-e6aa9c21d6f0",
                    "name" to "Pixel 3a",
                    "end" to "2020-11-12T16:02:00.000+01:00"
                ))
        )
    }
}