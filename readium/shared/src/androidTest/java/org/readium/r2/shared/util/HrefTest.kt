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
        assertEquals("/folder/", Href("", "/folder/").string)
        assertEquals("/", Href("/", "/folder/").string)

        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "").string)
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/").string)
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/file.txt").string)
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/folder").string)
        assertEquals("/folder/foo/bar.txt", Href("foo/bar.txt", "/folder/").string)
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/file.txt").string
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder").string
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/").string
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder").string
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder/").string
        )

        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "").string)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/").string)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/file.txt").string)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/folder").string)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/folder/").string)
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/file.txt").string
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder").string
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/").string
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder").string
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/").string
        )

        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "").string)
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/").string)
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/file.txt").string)
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/folder").string)
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/folder/").string)
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/file.txt").string
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder").string
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/").string
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/file.txt").string
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder").string
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/").string
        )

        assertEquals("/bar.txt", Href("foo/../bar.txt", "").string)
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/").string)
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/file.txt").string)
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/folder").string)
        assertEquals("/folder/bar.txt", Href("foo/../bar.txt", "/folder/").string)
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/file.txt").string
        )
        assertEquals(
            "http://example.com/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder").string
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/").string
        )
        assertEquals(
            "file:///root/folder/file.txt/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/file.txt").string
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder").string
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/").string
        )

        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "/").string
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "https://example.com/").string
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "/").string
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "file://foo/").string
        )

        // Anchor and query parameters are preserved
        assertEquals("/foo/bar.txt#anchor", Href("foo/bar.txt#anchor", "/").string)
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").string
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").string
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").string
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href("file:///root/foo/bar.txt?query=param#anchor", "/").string
        )

        assertEquals("/foo/bar.txt#anchor", Href("foo/bar.txt#anchor", "/").string)
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").string
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").string
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").string
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href("file:///root/foo/bar.txt?query=param#anchor", "/").string
        )

        // HREF that is just an anchor
        assertEquals("/#anchor", Href("#anchor", "").string)
        assertEquals("/#anchor", Href("#anchor", "/").string)
        assertEquals("/file.txt#anchor", Href("#anchor", "/file.txt").string)
        assertEquals("/folder#anchor", Href("#anchor", "/folder").string)
        assertEquals("/folder/#anchor", Href("#anchor", "/folder/").string)
        assertEquals(
            "http://example.com/folder/file.txt#anchor",
            Href("#anchor", "http://example.com/folder/file.txt").string
        )
        assertEquals(
            "http://example.com/folder#anchor",
            Href("#anchor", "http://example.com/folder").string
        )
        assertEquals(
            "http://example.com/folder/#anchor",
            Href("#anchor", "http://example.com/folder/").string
        )
        assertEquals(
            "file:///root/folder/file.txt#anchor",
            Href("#anchor", "file:///root/folder/file.txt").string
        )
        assertEquals(
            "file:///root/folder#anchor",
            Href("#anchor", "file:///root/folder").string
        )
        assertEquals(
            "file:///root/folder/#anchor",
            Href("#anchor", "file:///root/folder/").string
        )

        // HREF containing spaces.
        assertEquals("/foo bar.txt", Href("foo bar.txt", "").string)
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/").string)
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/file.txt").string)
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/base folder").string)
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "/base folder/").string
        )
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "/base folder/file.txt").string
        )
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "base folder/file.txt").string
        )

        // HREF containing special characters
        assertEquals(
            "/base%folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "/base%folder/").string
        )
        assertEquals(
            "/base folder/foo bar/baz%qux.txt",
            Href("foo%20bar/baz%25qux.txt", "/base%20folder/").string
        )
        assertEquals(
            "http://example.com/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder").string
        )
        assertEquals(
            "http://example.com/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder/").string
        )
        assertEquals(
            "http://example.com/base folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "http://example.com/base%20folder/").string
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "file:///root/base%20folder").string
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "file:///root/base%20folder/").string
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "file:///root/base%20folder/").string
        )
        assertEquals(
            "/foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "/").string
        )
        assertEquals(
            "http://example.com/foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "http://example.com/").string
        )
        assertEquals(
            "http://example.com/foo bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "http://example.com/").string
        )
        assertEquals(
            "http://absolute.com/foo bar.txt?query=param#Hello world £500",
            Href(
                "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).string
        )
        assertEquals(
            "http://absolute.com/foo bar.txt?query=param#Hello world £500",
            Href("http://absolute.com/foo bar.txt?query=param#Hello world £500", "/").string
        )
        assertEquals(
            "file:///foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "file:///root/").string
        )
        assertEquals(
            "file:///foo bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "file:///root/").string
        )
        assertEquals(
            "file:///root/foo bar.txt?query=param#Hello world £500",
            Href(
                "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).string
        )
        assertEquals(
            "file:///root/foo bar.txt?query=param#Hello world £500",
            Href("file:///root/foo bar.txt?query=param#Hello world £500", "/").string
        )
    }

    @Test
    fun getPercentEncodedString() {
        assertEquals("/folder/", Href("", "/folder/").percentEncodedString)
        assertEquals("/", Href("/", "/folder/").percentEncodedString)

        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "").percentEncodedString)
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/").percentEncodedString)
        assertEquals(
            "/foo/bar.txt",
            Href("foo/bar.txt", "/file.txt").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt",
            Href("foo/bar.txt", "/folder").percentEncodedString
        )
        assertEquals(
            "/folder/foo/bar.txt",
            Href("foo/bar.txt", "/folder/").percentEncodedString
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href(
                "foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).percentEncodedString
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder").percentEncodedString
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/").percentEncodedString
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder").percentEncodedString
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder/").percentEncodedString
        )

        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "").percentEncodedString)
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/").percentEncodedString)
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/file.txt").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/folder").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/folder/").percentEncodedString
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href(
                "/foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).percentEncodedString
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder").percentEncodedString
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/").percentEncodedString
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/file.txt").percentEncodedString
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder").percentEncodedString
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/").percentEncodedString
        )

        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "").percentEncodedString)
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/file.txt").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/folder").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/folder/").percentEncodedString
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href(
                "../foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).percentEncodedString
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder").percentEncodedString
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/").percentEncodedString
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href(
                "../foo/bar.txt",
                "file:///root/folder/file.txt"
            ).percentEncodedString
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder").percentEncodedString
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/").percentEncodedString
        )

        assertEquals("/bar.txt", Href("foo/../bar.txt", "").percentEncodedString)
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/").percentEncodedString)
        assertEquals(
            "/bar.txt",
            Href("foo/../bar.txt", "/file.txt").percentEncodedString
        )
        assertEquals(
            "/bar.txt",
            Href("foo/../bar.txt", "/folder").percentEncodedString
        )
        assertEquals(
            "/folder/bar.txt",
            Href("foo/../bar.txt", "/folder/").percentEncodedString
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href(
                "foo/../bar.txt",
                "http://example.com/folder/file.txt"
            ).percentEncodedString
        )
        assertEquals(
            "http://example.com/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder").percentEncodedString
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/").percentEncodedString
        )
        assertEquals(
            "file:///root/folder/file.txt/bar.txt",
            Href(
                "foo/../bar.txt",
                "file:///root/folder/file.txt"
            ).percentEncodedString
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder").percentEncodedString
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/").percentEncodedString
        )

        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "/").percentEncodedString
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "https://example.com/").percentEncodedString
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "/").percentEncodedString
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "file:///root/").percentEncodedString
        )

        // Anchor and query parameters are preserved
        assertEquals(
            "/foo/bar.txt#anchor",
            Href("foo/bar.txt#anchor", "/").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").percentEncodedString
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").percentEncodedString
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href(
                "file:///root/foo/bar.txt?query=param#anchor",
                "/"
            ).percentEncodedString
        )

        assertEquals(
            "/foo/bar.txt#anchor",
            Href("foo/bar.txt#anchor", "/").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").percentEncodedString
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").percentEncodedString
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").percentEncodedString
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href(
                "file:///root/foo/bar.txt?query=param#anchor",
                "/"
            ).percentEncodedString
        )

        // HREF that is just an anchor
        assertEquals("/#anchor", Href("#anchor", "").percentEncodedString)
        assertEquals("/#anchor", Href("#anchor", "/").percentEncodedString)
        assertEquals(
            "/file.txt#anchor",
            Href("#anchor", "/file.txt").percentEncodedString
        )
        assertEquals(
            "/folder#anchor",
            Href("#anchor", "/folder").percentEncodedString
        )
        assertEquals(
            "/folder/#anchor",
            Href("#anchor", "/folder/").percentEncodedString
        )
        assertEquals(
            "http://example.com/folder/file.txt#anchor",
            Href(
                "#anchor",
                "http://example.com/folder/file.txt"
            ).percentEncodedString
        )
        assertEquals(
            "http://example.com/folder#anchor",
            Href("#anchor", "http://example.com/folder").percentEncodedString
        )
        assertEquals(
            "http://example.com/folder/#anchor",
            Href("#anchor", "http://example.com/folder/").percentEncodedString
        )
        assertEquals(
            "file:///root/folder/file.txt#anchor",
            Href("#anchor", "file:///root/folder/file.txt").percentEncodedString
        )
        assertEquals(
            "file:///root/folder#anchor",
            Href("#anchor", "file:///root/folder").percentEncodedString
        )
        assertEquals(
            "file:///root/folder/#anchor",
            Href("#anchor", "file:///root/folder/").percentEncodedString
        )

        // HREF containing spaces.
        assertEquals("/foo%20bar.txt", Href("foo bar.txt", "").percentEncodedString)
        assertEquals("/foo%20bar.txt", Href("foo bar.txt", "/").percentEncodedString)
        assertEquals(
            "/foo%20bar.txt",
            Href("foo bar.txt", "/file.txt").percentEncodedString
        )
        assertEquals(
            "/foo%20bar.txt",
            Href("foo bar.txt", "/base folder").percentEncodedString
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "/base folder/").percentEncodedString
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "/base folder/file.txt").percentEncodedString
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "base folder/file.txt").percentEncodedString
        )

        // HREF containing special characters
        assertEquals(
            "/base%25folder/foo%20bar/baz%25qux.txt",
            Href("foo bar/baz%qux.txt", "/base%folder/").percentEncodedString
        )
        assertEquals(
            "/base%20folder/foo%20bar/baz%25qux.txt",
            Href("foo%20bar/baz%25qux.txt", "/base%20folder/").percentEncodedString
        )
        assertEquals(
            "http://example.com/foo%20bar/baz%20qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder").percentEncodedString
        )
        assertEquals(
            "http://example.com/base%20folder/foo%20bar/baz%20qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder/").percentEncodedString
        )
        assertEquals(
            "http://example.com/base%20folder/foo%20bar/baz%25qux.txt",
            Href("foo bar/baz%qux.txt", "http://example.com/base%20folder/").percentEncodedString
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%20qux.txt",
            Href(
                "foo bar/baz qux.txt",
                "file:///root/base%20folder"
            ).percentEncodedString
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%20qux.txt",
            Href(
                "foo bar/baz qux.txt",
                "file:///root/base%20folder/"
            ).percentEncodedString
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%25qux.txt",
            Href(
                "foo bar/baz%qux.txt",
                "file:///root/base%20folder/"
            ).percentEncodedString
        )
        assertEquals(
            "/foo%20bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "/").percentEncodedString
        )
        assertEquals(
            "http://example.com/foo%20bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "http://example.com/").percentEncodedString
        )
        assertEquals(
            "http://example.com/foo%20bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "http://example.com/").percentEncodedString
        )
        assertEquals(
            "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href(
                "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).percentEncodedString
        )
        assertEquals(
            "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href("http://absolute.com/foo bar.txt?query=param#Hello world £500", "/").percentEncodedString
        )
        assertEquals(
            "file:///foo%20bar.txt?query=param#anchor",
            Href(
                "/foo bar.txt?query=param#anchor",
                "file:///root/"
            ).percentEncodedString
        )
        assertEquals(
            "file:///foo%20bar.txt?query=param#anchor",
            Href(
                "/foo%20bar.txt?query=param#anchor",
                "file:///root/"
            ).percentEncodedString
        )
        assertEquals(
            "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href(
                "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).percentEncodedString
        )
        assertEquals(
            "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href("file:///root/foo bar.txt?query=param#Hello world £500", "/").percentEncodedString
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
