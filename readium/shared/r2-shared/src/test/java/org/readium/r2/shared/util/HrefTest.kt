/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.util.Href.QueryParameter

class HrefTest {

    @Test
    fun `normalize to base HREF`() {
        assertEquals(Href("", "/folder/").string, "/folder/")
        assertEquals(Href("/", "/folder/").string, "/")

        assertEquals(Href("foo/bar.txt", "").string, "/foo/bar.txt")
        assertEquals(Href("foo/bar.txt", "/").string, "/foo/bar.txt")
        assertEquals(Href("foo/bar.txt", "/file.txt").string, "/foo/bar.txt")
        assertEquals(Href("foo/bar.txt", "/folder").string, "/foo/bar.txt")
        assertEquals(Href("foo/bar.txt", "/folder/").string, "/folder/foo/bar.txt")
        assertEquals(Href("foo/bar.txt", "http://example.com/folder/file.txt").string, "http://example.com/folder/foo/bar.txt")
        assertEquals(Href("foo/bar.txt", "http://example.com/folder").string, "http://example.com/foo/bar.txt")
        assertEquals(Href("foo/bar.txt", "http://example.com/folder/").string, "http://example.com/folder/foo/bar.txt")

        assertEquals(Href("/foo/bar.txt", "").string, "/foo/bar.txt")
        assertEquals(Href("/foo/bar.txt", "/").string, "/foo/bar.txt")
        assertEquals(Href("/foo/bar.txt", "/file.txt").string, "/foo/bar.txt")
        assertEquals(Href("/foo/bar.txt", "/folder").string, "/foo/bar.txt")
        assertEquals(Href("/foo/bar.txt", "/folder/").string, "/foo/bar.txt")
        assertEquals(Href("/foo/bar.txt", "http://example.com/folder/file.txt").string, "http://example.com/foo/bar.txt")
        assertEquals(Href("/foo/bar.txt", "http://example.com/folder").string, "http://example.com/foo/bar.txt")
        assertEquals(Href("/foo/bar.txt", "http://example.com/folder/").string, "http://example.com/foo/bar.txt")

        assertEquals(Href("../foo/bar.txt", "").string, "/../foo/bar.txt")
        assertEquals(Href("../foo/bar.txt", "/").string, "/../foo/bar.txt")
        assertEquals(Href("../foo/bar.txt", "/file.txt").string, "/../foo/bar.txt")
        assertEquals(Href("../foo/bar.txt", "/folder").string, "/../foo/bar.txt")
        assertEquals(Href("../foo/bar.txt", "/folder/").string, "/foo/bar.txt")
        assertEquals(Href("../foo/bar.txt", "http://example.com/folder/file.txt").string, "http://example.com/foo/bar.txt")
        assertEquals(Href("../foo/bar.txt", "http://example.com/folder").string, "http://example.com/../foo/bar.txt")
        assertEquals(Href("../foo/bar.txt", "http://example.com/folder/").string, "http://example.com/foo/bar.txt")

        assertEquals(Href("foo/../bar.txt", "").string, "/bar.txt")
        assertEquals(Href("foo/../bar.txt", "/").string, "/bar.txt")
        assertEquals(Href("foo/../bar.txt", "/file.txt").string, "/bar.txt")
        assertEquals(Href("foo/../bar.txt", "/folder").string, "/bar.txt")
        assertEquals(Href("foo/../bar.txt", "/folder/").string, "/folder/bar.txt")
        assertEquals(Href("foo/../bar.txt", "http://example.com/folder/file.txt").string, "http://example.com/folder/bar.txt")
        assertEquals(Href("foo/../bar.txt", "http://example.com/folder").string, "http://example.com/bar.txt")
        assertEquals(Href("foo/../bar.txt", "http://example.com/folder/").string, "http://example.com/folder/bar.txt")

        assertEquals(Href("http://absolute.com/foo/bar.txt", "/").string, "http://absolute.com/foo/bar.txt")
        assertEquals(Href("http://absolute.com/foo/bar.txt", "https://example.com/").string, "http://absolute.com/foo/bar.txt")

        // Anchor and query parameters are preserved
        assertEquals(Href("foo/bar.txt#anchor", "/").string, "/foo/bar.txt#anchor")
        assertEquals(Href("foo/bar.txt?query=param#anchor", "/").string, "/foo/bar.txt?query=param#anchor")
        assertEquals(Href("/foo/bar.txt?query=param#anchor", "/").string, "/foo/bar.txt?query=param#anchor")
        assertEquals(Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").string, "http://absolute.com/foo/bar.txt?query=param#anchor")

        assertEquals(Href("foo/bar.txt#anchor", "/").string, "/foo/bar.txt#anchor")
        assertEquals(Href("foo/bar.txt?query=param#anchor", "/").string, "/foo/bar.txt?query=param#anchor")
        assertEquals(Href("/foo/bar.txt?query=param#anchor", "/").string, "/foo/bar.txt?query=param#anchor")
        assertEquals(Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").string, "http://absolute.com/foo/bar.txt?query=param#anchor")

        // HREF that is just an anchor
        assertEquals(Href("#anchor", "").string, "/#anchor")
        assertEquals(Href("#anchor", "/").string, "/#anchor")
        assertEquals(Href("#anchor", "/file.txt").string, "/file.txt#anchor")
        assertEquals(Href("#anchor", "/folder").string, "/folder#anchor")
        assertEquals(Href("#anchor", "/folder/").string, "/folder/#anchor")
        assertEquals(Href("#anchor", "http://example.com/folder/file.txt").string, "http://example.com/folder/file.txt#anchor")
        assertEquals(Href("#anchor", "http://example.com/folder").string, "http://example.com/folder#anchor")
        assertEquals(Href("#anchor", "http://example.com/folder/").string, "http://example.com/folder/#anchor")

        // Percent encoding
        assertEquals(Href("http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500", "/").string, "http://absolute.com/foo bar.txt?query=param#Hello world £500")
        assertEquals(Href("http://absolute.com/foo bar.txt?query=param#Hello world £500", "/").string, "http://absolute.com/foo bar.txt?query=param#Hello world £500")
    }

    @Test
    fun `get percent-encoded string`() {
        assertEquals(
            "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href("http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500", "/").percentEncodedString
        )
        assertEquals(
            "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href("http://absolute.com/foo bar.txt?query=param#Hello world £500", "/").percentEncodedString
        )
    }

    // Needs to be run as an instrumented test
//    @Test
//    fun `get query parameters`() {
//        assertEquals(Href("http://domain.com/path").queryParameters, emptyList<QueryParameter>())
//        assertEquals(Href("http://domain.com/path?query=param#anchor").queryParameters, listOf(
//            QueryParameter(name = "query", value = "param")
//        ))
//        assertEquals(Href("http://domain.com/path?query=param&fruit=banana&query=other&empty").queryParameters, listOf(
//            QueryParameter(name = "query", value = "param"),
//            QueryParameter(name = "fruit", value = "banana"),
//            QueryParameter(name = "query", value = "other"),
//            QueryParameter(name = "empty", value = null)
//        ))
//    }

    @Test
    fun `get first parameter named x`() {
        val params = listOf(
            QueryParameter(name = "query", value = "param"),
            QueryParameter(name = "fruit", value = "banana"),
            QueryParameter(name = "query", value = "other"),
            QueryParameter(name = "empty", value = null)
        )

        assertEquals(params.firstNamedOrNull("query"), "param")
        assertEquals(params.firstNamedOrNull("fruit"), "banana")
        assertNull(params.firstNamedOrNull("empty"))
        assertNull(params.firstNamedOrNull("not-found"))
    }

    @Test
    fun `get all parameters named x`() {
        val params = listOf(
            QueryParameter(name = "query", value = "param"),
            QueryParameter(name = "fruit", value = "banana"),
            QueryParameter(name = "query", value = "other"),
            QueryParameter(name = "empty", value = null)
        )

        assertEquals(params.allNamed("query"), listOf("param", "other"))
        assertEquals(params.allNamed("fruit"), listOf("banana"))
        assertEquals(params.allNamed("empty"), emptyList<QueryParameter>())
        assertEquals(params.allNamed("not-found"), emptyList<QueryParameter>())
    }

}