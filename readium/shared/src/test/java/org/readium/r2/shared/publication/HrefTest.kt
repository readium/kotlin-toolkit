/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HrefTest {

    private val base = Url("http://readium/publication/")!!

    @Test
    fun `convert static Href to Url`() {
        val url = Url("folder/chapter.xhtml")!!
        assertEquals(url, Href(url).resolve())
        assertEquals(
            Url("http://readium/publication/folder/chapter.xhtml"),
            Href(url).resolve(base)
        )
        // Parameters are ignored.
        assertEquals(url, Href(url).resolve(parameters = mapOf("a" to "b")))
    }

    @Test
    fun `convert templated Href to Url`() {
        val template = Href("url{?x,hello,y}name", templated = true)!!

        val parameters = mapOf(
            "x" to "aaa",
            "hello" to "Hello, world",
            "y" to "b",
            "foo" to "bar"
        )

        assertEquals(
            Url("url?x=&hello=&y=name"),
            template.resolve()
        )

        assertEquals(
            Url("http://readium/publication/url?x=&hello=&y=name"),
            template.resolve(base)
        )

        assertEquals(
            Url("url?x=aaa&hello=Hello,%20world&y=bname"),
            template.resolve(parameters = parameters)
        )

        assertEquals(
            Url("http://readium/publication/url?x=aaa&hello=Hello,%20world&y=bname"),
            template.resolve(base, parameters)
        )
    }

    @Test
    fun `get is templated`() {
        assertFalse(Href("url", templated = false)!!.isTemplated)
        assertTrue(Href("url", templated = true)!!.isTemplated)
        assertTrue(Href("url{?x,hello,y}name", templated = true)!!.isTemplated)
    }

    @Test
    fun `get template parameters`() {
        assertNull(Href("url", templated = false)!!.parameters)

        assertEquals(
            listOf<String>(),
            Href("url", templated = true)!!.parameters
        )
        assertEquals(
            listOf("x", "hello", "y"),
            Href("url{?x,hello,y}name", templated = true)!!.parameters
        )
    }

    @Test
    fun getToString() {
        assertEquals("folder/chapter.xhtml", Href(Url("folder/chapter.xhtml")!!).toString())
        assertEquals(
            "url{?x,hello,y}name",
            Href("url{?x,hello,y}name", templated = true)!!.toString()
        )
    }

    @Test
    fun equality() {
        val url1 = Url("folder/chapter1.xhtml")!!
        val url2 = Url("folder/chapter2.xhtml")!!
        assertEquals(Href(url1), Href(url1))
        assertNotEquals(Href(url1), Href(url2))

        assertEquals(Href("template1", templated = true), Href("template1", templated = true)!!)
        assertNotEquals(Href("template1", templated = true), Href("template2", templated = true)!!)
    }
}
