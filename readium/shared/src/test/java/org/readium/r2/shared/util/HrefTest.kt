/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HrefTest {

    private val base = Url("http://readium/publication/")!!

    @Test
    fun `convert UrlHref to Url`() {
        val url = Url("folder/chapter.xhtml")!!
        assertEquals(url, UrlHref(url).toUrl())
        assertEquals(
            Url("http://readium/publication/folder/chapter.xhtml"),
            UrlHref(url).toUrl(base)
        )
        // Parameters are ignored.
        assertEquals(url, UrlHref(url).toUrl(parameters = mapOf("a" to "b")))
    }

    @Test
    fun `convert TemplatedHref to Url`() {
        val template = TemplatedHref("url{?x,hello,y}name")

        val parameters = mapOf(
            "x" to "aaa",
            "hello" to "Hello, world",
            "y" to "b",
            "foo" to "bar"
        )

        assertEquals(
            Url("url?x=&hello=&y=name"),
            template.toUrl()
        )

        assertEquals(
            Url("http://readium/publication/url?x=&hello=&y=name"),
            template.toUrl(base)
        )

        assertEquals(
            Url("url?x=aaa&hello=Hello,%20world&y=bname"),
            template.toUrl(parameters = parameters)
        )

        assertEquals(
            Url("http://readium/publication/url?x=aaa&hello=Hello,%20world&y=bname"),
            template.toUrl(base, parameters)
        )
    }
}
