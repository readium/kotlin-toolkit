/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.shared.util.Href.QueryParameter

class HrefTest {

    @Test
    fun normalizeToBaseHref() {
        assertEquals("/folder/", Href("", "/folder/").value)
        assertEquals("/", Href("/", "/folder/").value)

        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "").value)
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/").value)
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/file.txt").value)
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/folder").value)
        assertEquals("/folder/foo/bar.txt", Href("foo/bar.txt", "/folder/").value)
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/file.txt").value
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder").value
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/").value
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder").value
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder/").value
        )

        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "").value)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/").value)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/file.txt").value)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/folder").value)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/folder/").value)
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/file.txt").value
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder").value
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/").value
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder").value
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/").value
        )

        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "").value)
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/").value)
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/file.txt").value)
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/folder").value)
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/folder/").value)
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/file.txt").value
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder").value
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/").value
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/file.txt").value
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder").value
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/").value
        )

        assertEquals("/bar.txt", Href("foo/../bar.txt", "").value)
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/").value)
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/file.txt").value)
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/folder").value)
        assertEquals("/folder/bar.txt", Href("foo/../bar.txt", "/folder/").value)
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/file.txt").value
        )
        assertEquals(
            "http://example.com/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder").value
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/").value
        )
        assertEquals(
            "file:///root/folder/file.txt/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/file.txt").value
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder").value
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/").value
        )

        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "/").value
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "https://example.com/").value
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "/").value
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "file://foo/").value
        )

        // Anchor and query parameters are preserved
        assertEquals("/foo/bar.txt#anchor", Href("foo/bar.txt#anchor", "/").value)
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").value
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").value
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").value
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href("file:///root/foo/bar.txt?query=param#anchor", "/").value
        )

        assertEquals("/foo/bar.txt#anchor", Href("foo/bar.txt#anchor", "/").value)
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").value
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").value
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").value
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href("file:///root/foo/bar.txt?query=param#anchor", "/").value
        )

        // HREF that is just an anchor
        assertEquals("/#anchor", Href("#anchor", "").value)
        assertEquals("/#anchor", Href("#anchor", "/").value)
        assertEquals("/file.txt#anchor", Href("#anchor", "/file.txt").value)
        assertEquals("/folder#anchor", Href("#anchor", "/folder").value)
        assertEquals("/folder/#anchor", Href("#anchor", "/folder/").value)
        assertEquals(
            "http://example.com/folder/file.txt#anchor",
            Href("#anchor", "http://example.com/folder/file.txt").value
        )
        assertEquals(
            "http://example.com/folder#anchor",
            Href("#anchor", "http://example.com/folder").value
        )
        assertEquals(
            "http://example.com/folder/#anchor",
            Href("#anchor", "http://example.com/folder/").value
        )
        assertEquals(
            "file:///root/folder/file.txt#anchor",
            Href("#anchor", "file:///root/folder/file.txt").value
        )
        assertEquals(
            "file:///root/folder#anchor",
            Href("#anchor", "file:///root/folder").value
        )
        assertEquals(
            "file:///root/folder/#anchor",
            Href("#anchor", "file:///root/folder/").value
        )

        // HREF containing spaces.
        assertEquals("/foo bar.txt", Href("foo bar.txt", "").value)
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/").value)
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/file.txt").value)
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/base folder").value)
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "/base folder/").value
        )
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "/base folder/file.txt").value
        )
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "base folder/file.txt").value
        )

        // HREF containing special characters
        assertEquals(
            "/base%folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "/base%folder/").value
        )
        assertEquals(
            "/base folder/foo bar/baz%qux.txt",
            Href("foo%20bar/baz%25qux.txt", "/base%20folder/").value
        )
        assertEquals(
            "http://example.com/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder").value
        )
        assertEquals(
            "http://example.com/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder/").value
        )
        assertEquals(
            "http://example.com/base folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "http://example.com/base%20folder/").value
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "file:///root/base%20folder").value
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "file:///root/base%20folder/").value
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "file:///root/base%20folder/").value
        )
        assertEquals(
            "/foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "/").value
        )
        assertEquals(
            "http://example.com/foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "http://example.com/").value
        )
        assertEquals(
            "http://example.com/foo bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "http://example.com/").value
        )
        assertEquals(
            "http://absolute.com/foo bar.txt?query=param#Hello world £500",
            Href(
                "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).value
        )
        assertEquals(
            "http://absolute.com/foo bar.txt?query=param#Hello world £500",
            Href("http://absolute.com/foo bar.txt?query=param#Hello world £500", "/").value
        )
        assertEquals(
            "file:///foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "file:///root/").value
        )
        assertEquals(
            "file:///foo bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "file:///root/").value
        )
        assertEquals(
            "file:///root/foo bar.txt?query=param#Hello world £500",
            Href(
                "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).value
        )
        assertEquals(
            "file:///root/foo bar.txt?query=param#Hello world £500",
            Href("file:///root/foo bar.txt?query=param#Hello world £500", "/").value
        )
    }

    @Test
    fun getPercentEncodedString() {
        assertEquals("/folder/", Href("", "/folder/").percentEncoded)
        assertEquals("/", Href("/", "/folder/").percentEncoded)

        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "").percentEncoded)
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/").percentEncoded)
        assertEquals(
            "/foo/bar.txt",
            Href("foo/bar.txt", "/file.txt").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt",
            Href("foo/bar.txt", "/folder").percentEncoded
        )
        assertEquals(
            "/folder/foo/bar.txt",
            Href("foo/bar.txt", "/folder/").percentEncoded
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href(
                "foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).percentEncoded
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder").percentEncoded
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/").percentEncoded
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder").percentEncoded
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder/").percentEncoded
        )

        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "").percentEncoded)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/").percentEncoded)
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/file.txt").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/folder").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/folder/").percentEncoded
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href(
                "/foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).percentEncoded
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder").percentEncoded
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/").percentEncoded
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/file.txt").percentEncoded
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder").percentEncoded
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/").percentEncoded
        )

        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "").percentEncoded)
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/file.txt").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/folder").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/folder/").percentEncoded
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href(
                "../foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).percentEncoded
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder").percentEncoded
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/").percentEncoded
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href(
                "../foo/bar.txt",
                "file:///root/folder/file.txt"
            ).percentEncoded
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder").percentEncoded
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/").percentEncoded
        )

        assertEquals("/bar.txt", Href("foo/../bar.txt", "").percentEncoded)
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/").percentEncoded)
        assertEquals(
            "/bar.txt",
            Href("foo/../bar.txt", "/file.txt").percentEncoded
        )
        assertEquals(
            "/bar.txt",
            Href("foo/../bar.txt", "/folder").percentEncoded
        )
        assertEquals(
            "/folder/bar.txt",
            Href("foo/../bar.txt", "/folder/").percentEncoded
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href(
                "foo/../bar.txt",
                "http://example.com/folder/file.txt"
            ).percentEncoded
        )
        assertEquals(
            "http://example.com/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder").percentEncoded
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/").percentEncoded
        )
        assertEquals(
            "file:///root/folder/file.txt/bar.txt",
            Href(
                "foo/../bar.txt",
                "file:///root/folder/file.txt"
            ).percentEncoded
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder").percentEncoded
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/").percentEncoded
        )

        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "/").percentEncoded
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "https://example.com/").percentEncoded
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "/").percentEncoded
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "file:///root/").percentEncoded
        )

        // Anchor and query parameters are preserved
        assertEquals(
            "/foo/bar.txt#anchor",
            Href("foo/bar.txt#anchor", "/").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").percentEncoded
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").percentEncoded
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href(
                "file:///root/foo/bar.txt?query=param#anchor",
                "/"
            ).percentEncoded
        )

        assertEquals(
            "/foo/bar.txt#anchor",
            Href("foo/bar.txt#anchor", "/").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").percentEncoded
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").percentEncoded
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").percentEncoded
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href(
                "file:///root/foo/bar.txt?query=param#anchor",
                "/"
            ).percentEncoded
        )

        // HREF that is just an anchor
        assertEquals("/#anchor", Href("#anchor", "").percentEncoded)
        assertEquals("/#anchor", Href("#anchor", "/").percentEncoded)
        assertEquals(
            "/file.txt#anchor",
            Href("#anchor", "/file.txt").percentEncoded
        )
        assertEquals(
            "/folder#anchor",
            Href("#anchor", "/folder").percentEncoded
        )
        assertEquals(
            "/folder/#anchor",
            Href("#anchor", "/folder/").percentEncoded
        )
        assertEquals(
            "http://example.com/folder/file.txt#anchor",
            Href(
                "#anchor",
                "http://example.com/folder/file.txt"
            ).percentEncoded
        )
        assertEquals(
            "http://example.com/folder#anchor",
            Href("#anchor", "http://example.com/folder").percentEncoded
        )
        assertEquals(
            "http://example.com/folder/#anchor",
            Href("#anchor", "http://example.com/folder/").percentEncoded
        )
        assertEquals(
            "file:///root/folder/file.txt#anchor",
            Href("#anchor", "file:///root/folder/file.txt").percentEncoded
        )
        assertEquals(
            "file:///root/folder#anchor",
            Href("#anchor", "file:///root/folder").percentEncoded
        )
        assertEquals(
            "file:///root/folder/#anchor",
            Href("#anchor", "file:///root/folder/").percentEncoded
        )

        // HREF containing spaces.
        assertEquals("/foo%20bar.txt", Href("foo bar.txt", "").percentEncoded)
        assertEquals("/foo%20bar.txt", Href("foo bar.txt", "/").percentEncoded)
        assertEquals(
            "/foo%20bar.txt",
            Href("foo bar.txt", "/file.txt").percentEncoded
        )
        assertEquals(
            "/foo%20bar.txt",
            Href("foo bar.txt", "/base folder").percentEncoded
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "/base folder/").percentEncoded
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "/base folder/file.txt").percentEncoded
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "base folder/file.txt").percentEncoded
        )

        // HREF containing special characters
        assertEquals(
            "/base%25folder/foo%20bar/baz%25qux.txt",
            Href("foo bar/baz%qux.txt", "/base%folder/").percentEncoded
        )
        assertEquals(
            "/base%20folder/foo%20bar/baz%25qux.txt",
            Href("foo%20bar/baz%25qux.txt", "/base%20folder/").percentEncoded
        )
        assertEquals(
            "http://example.com/foo%20bar/baz%20qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder").percentEncoded
        )
        assertEquals(
            "http://example.com/base%20folder/foo%20bar/baz%20qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder/").percentEncoded
        )
        assertEquals(
            "http://example.com/base%20folder/foo%20bar/baz%25qux.txt",
            Href("foo bar/baz%qux.txt", "http://example.com/base%20folder/").percentEncoded
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%20qux.txt",
            Href(
                "foo bar/baz qux.txt",
                "file:///root/base%20folder"
            ).percentEncoded
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%20qux.txt",
            Href(
                "foo bar/baz qux.txt",
                "file:///root/base%20folder/"
            ).percentEncoded
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%25qux.txt",
            Href(
                "foo bar/baz%qux.txt",
                "file:///root/base%20folder/"
            ).percentEncoded
        )
        assertEquals(
            "/foo%20bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "/").percentEncoded
        )
        assertEquals(
            "http://example.com/foo%20bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "http://example.com/").percentEncoded
        )
        assertEquals(
            "http://example.com/foo%20bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "http://example.com/").percentEncoded
        )
        assertEquals(
            "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href(
                "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).percentEncoded
        )
        assertEquals(
            "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href("http://absolute.com/foo bar.txt?query=param#Hello world £500", "/").percentEncoded
        )
        assertEquals(
            "file:///foo%20bar.txt?query=param#anchor",
            Href(
                "/foo bar.txt?query=param#anchor",
                "file:///root/"
            ).percentEncoded
        )
        assertEquals(
            "file:///foo%20bar.txt?query=param#anchor",
            Href(
                "/foo%20bar.txt?query=param#anchor",
                "file:///root/"
            ).percentEncoded
        )
        assertEquals(
            "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href(
                "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).percentEncoded
        )
        assertEquals(
            "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href("file:///root/foo bar.txt?query=param#Hello world £500", "/").percentEncoded
        )
    }

    @Test
    fun getQueryParameters() {
        assertEquals(emptyList<QueryParameter>(), Href("http://domain.com/path").queryParameters)
        assertEquals(listOf(QueryParameter(name = "query", value = "param")), Href("http://domain.com/path?query=param#anchor").queryParameters)
        assertEquals(
            listOf(
                QueryParameter(name = "query", value = "param"),
                QueryParameter(name = "fruit", value = "banana"),
                QueryParameter(name = "query", value = "other"),
                QueryParameter(name = "empty", value = null)
            ),
            Href("http://domain.com/path?query=param&fruit=banana&query=other&empty").queryParameters
        )
    }

    @Test
    fun getFirstParameterNamedX() {
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
    fun getAllParametersNamedX() {
        val params = listOf(
            QueryParameter(name = "query", value = "param"),
            QueryParameter(name = "fruit", value = "banana"),
            QueryParameter(name = "query", value = "other"),
            QueryParameter(name = "empty", value = null)
        )

        assertEquals(params.allNamed("query"), listOf("param", "other"))
        assertEquals(params.allNamed("fruit"), listOf("banana"))
        assertEquals(params.allNamed("empty"), emptyList<String>())
        assertEquals(params.allNamed("not-found"), emptyList<String>())
    }
}
