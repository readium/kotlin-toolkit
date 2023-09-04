/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import kotlin.test.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Url
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

    @Test
    fun `get TemplatedHref parameters`() {
        assertEquals(
            listOf<String>(),
            TemplatedHref("url").parameters
        )
        assertEquals(
            listOf("x", "hello", "y"),
            TemplatedHref("url{?x,hello,y}name").parameters
        )
    }

    @Test
    fun getToString() {
        assertEquals("folder/chapter.xhtml", UrlHref(Url("folder/chapter.xhtml")!!).toString())
        assertEquals("url{?x,hello,y}name", TemplatedHref("url{?x,hello,y}name").toString())
    }

    @Test
    fun equality() {
        val url1 = Url("folder/chapter1.xhtml")!!
        val url2 = Url("folder/chapter2.xhtml")!!
        assertEquals(UrlHref(url1), UrlHref(url1))
        assertNotEquals(UrlHref(url1), UrlHref(url2))

        assertEquals(TemplatedHref("template1"), TemplatedHref("template1"))
        assertNotEquals(TemplatedHref("template1"), TemplatedHref("template2"))
    }

    @Test
    fun cannotCompareToUrl() {
        val url = Url("folder/chapter.xhtml")!!

        assertFailsWith(IllegalArgumentException::class) {
            assertFalse(UrlHref(url) as Href == url)
        }
        assertFailsWith(IllegalArgumentException::class) {
            assertFalse(TemplatedHref("") as Href == url)
        }
        assertFailsWith(IllegalArgumentException::class) {
            assertFalse(url == UrlHref(url) as Href)
        }
        assertFailsWith(IllegalArgumentException::class) {
            assertFalse(url == TemplatedHref("") as Href)
        }
    }
}
