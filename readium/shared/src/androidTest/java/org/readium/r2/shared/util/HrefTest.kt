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
        assertEquals("/folder/", Href("", "/folder/").absoluteHref())
        assertEquals("/", Href("/", "/folder/").absoluteHref())

        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "").absoluteHref())
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/").absoluteHref())
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/file.txt").absoluteHref())
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/folder").absoluteHref())
        assertEquals("/folder/foo/bar.txt", Href("foo/bar.txt", "/folder/").absoluteHref())
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/file.txt").absoluteHref()
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder").absoluteHref()
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/").absoluteHref()
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder").absoluteHref()
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder/").absoluteHref()
        )

        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "").absoluteHref())
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/").absoluteHref())
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/file.txt").absoluteHref())
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/folder").absoluteHref())
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/folder/").absoluteHref())
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/file.txt").absoluteHref()
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder").absoluteHref()
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/").absoluteHref()
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder").absoluteHref()
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/").absoluteHref()
        )

        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "").absoluteHref())
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/").absoluteHref())
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/file.txt").absoluteHref())
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/folder").absoluteHref())
        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "/folder/").absoluteHref())
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/file.txt").absoluteHref()
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder").absoluteHref()
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/").absoluteHref()
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/file.txt").absoluteHref()
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder").absoluteHref()
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/").absoluteHref()
        )

        assertEquals("/bar.txt", Href("foo/../bar.txt", "").absoluteHref())
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/").absoluteHref())
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/file.txt").absoluteHref())
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/folder").absoluteHref())
        assertEquals("/folder/bar.txt", Href("foo/../bar.txt", "/folder/").absoluteHref())
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/file.txt").absoluteHref()
        )
        assertEquals(
            "http://example.com/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder").absoluteHref()
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/").absoluteHref()
        )
        assertEquals(
            "file:///root/folder/file.txt/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/file.txt").absoluteHref()
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder").absoluteHref()
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/").absoluteHref()
        )

        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "/").absoluteHref()
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "https://example.com/").absoluteHref()
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "/").absoluteHref()
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "file://foo/").absoluteHref()
        )

        // Anchor and query parameters are preserved
        assertEquals("/foo/bar.txt#anchor", Href("foo/bar.txt#anchor", "/").absoluteHref())
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").absoluteHref()
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").absoluteHref()
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").absoluteHref()
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href("file:///root/foo/bar.txt?query=param#anchor", "/").absoluteHref()
        )

        assertEquals("/foo/bar.txt#anchor", Href("foo/bar.txt#anchor", "/").absoluteHref())
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").absoluteHref()
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").absoluteHref()
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").absoluteHref()
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href("file:///root/foo/bar.txt?query=param#anchor", "/").absoluteHref()
        )

        // HREF that is just an anchor
        assertEquals("/#anchor", Href("#anchor", "").absoluteHref())
        assertEquals("/#anchor", Href("#anchor", "/").absoluteHref())
        assertEquals("/file.txt#anchor", Href("#anchor", "/file.txt").absoluteHref())
        assertEquals("/folder#anchor", Href("#anchor", "/folder").absoluteHref())
        assertEquals("/folder/#anchor", Href("#anchor", "/folder/").absoluteHref())
        assertEquals(
            "http://example.com/folder/file.txt#anchor",
            Href("#anchor", "http://example.com/folder/file.txt").absoluteHref()
        )
        assertEquals(
            "http://example.com/folder#anchor",
            Href("#anchor", "http://example.com/folder").absoluteHref()
        )
        assertEquals(
            "http://example.com/folder/#anchor",
            Href("#anchor", "http://example.com/folder/").absoluteHref()
        )
        assertEquals(
            "file:///root/folder/file.txt#anchor",
            Href("#anchor", "file:///root/folder/file.txt").absoluteHref()
        )
        assertEquals(
            "file:///root/folder#anchor",
            Href("#anchor", "file:///root/folder").absoluteHref()
        )
        assertEquals(
            "file:///root/folder/#anchor",
            Href("#anchor", "file:///root/folder/").absoluteHref()
        )

        // HREF containing spaces.
        assertEquals("/foo bar.txt", Href("foo bar.txt", "").absoluteHref())
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/").absoluteHref())
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/file.txt").absoluteHref())
        assertEquals("/foo bar.txt", Href("foo bar.txt", "/base folder").absoluteHref())
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "/base folder/").absoluteHref()
        )
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "/base folder/file.txt").absoluteHref()
        )
        assertEquals(
            "/base folder/foo bar.txt",
            Href("foo bar.txt", "base folder/file.txt").absoluteHref()
        )

        // HREF containing special characters
        assertEquals(
            "/base%folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "/base%folder/").absoluteHref()
        )
        assertEquals(
            "/base folder/foo bar/baz%qux.txt",
            Href("foo%20bar/baz%25qux.txt", "/base%20folder/").absoluteHref()
        )
        assertEquals(
            "http://example.com/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder").absoluteHref()
        )
        assertEquals(
            "http://example.com/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder/").absoluteHref()
        )
        assertEquals(
            "http://example.com/base folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "http://example.com/base%20folder/").absoluteHref()
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "file:///root/base%20folder").absoluteHref()
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz qux.txt",
            Href("foo bar/baz qux.txt", "file:///root/base%20folder/").absoluteHref()
        )
        assertEquals(
            "file:///root/base folder/foo bar/baz%qux.txt",
            Href("foo bar/baz%qux.txt", "file:///root/base%20folder/").absoluteHref()
        )
        assertEquals(
            "/foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "/").absoluteHref()
        )
        assertEquals(
            "http://example.com/foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "http://example.com/").absoluteHref()
        )
        assertEquals(
            "http://example.com/foo bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "http://example.com/").absoluteHref()
        )
        assertEquals(
            "http://absolute.com/foo bar.txt?query=param#Hello world £500",
            Href(
                "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).absoluteHref()
        )
        assertEquals(
            "http://absolute.com/foo bar.txt?query=param#Hello world £500",
            Href("http://absolute.com/foo bar.txt?query=param#Hello world £500", "/").absoluteHref()
        )
        assertEquals(
            "file:///foo bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "file:///root/").absoluteHref()
        )
        assertEquals(
            "file:///foo bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "file:///root/").absoluteHref()
        )
        assertEquals(
            "file:///root/foo bar.txt?query=param#Hello world £500",
            Href(
                "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).absoluteHref()
        )
        assertEquals(
            "file:///root/foo bar.txt?query=param#Hello world £500",
            Href("file:///root/foo bar.txt?query=param#Hello world £500", "/").absoluteHref()
        )
    }

    @Test
    fun getPercentEncodedString() {
        assertEquals("/folder/", Href("", "/folder/").absoluteHref(percentEncoded = true))
        assertEquals("/", Href("/", "/folder/").absoluteHref(percentEncoded = true))

        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "").absoluteHref(percentEncoded = true))
        assertEquals("/foo/bar.txt", Href("foo/bar.txt", "/").absoluteHref(percentEncoded = true))
        assertEquals(
            "/foo/bar.txt",
            Href("foo/bar.txt", "/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt",
            Href("foo/bar.txt", "/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/folder/foo/bar.txt",
            Href("foo/bar.txt", "/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href(
                "foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/folder/foo/bar.txt",
            Href("foo/bar.txt", "http://example.com/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href("foo/bar.txt", "file:///root/folder/").absoluteHref(percentEncoded = true)
        )

        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "").absoluteHref(percentEncoded = true))
        assertEquals("/foo/bar.txt", Href("/foo/bar.txt", "/").absoluteHref(percentEncoded = true))
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt",
            Href("/foo/bar.txt", "/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href(
                "/foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("/foo/bar.txt", "http://example.com/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///foo/bar.txt",
            Href("/foo/bar.txt", "file:///root/folder/").absoluteHref(percentEncoded = true)
        )

        assertEquals("/foo/bar.txt", Href("../foo/bar.txt", "").absoluteHref(percentEncoded = true))
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt",
            Href("../foo/bar.txt", "/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href(
                "../foo/bar.txt",
                "http://example.com/folder/file.txt"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo/bar.txt",
            Href("../foo/bar.txt", "http://example.com/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder/foo/bar.txt",
            Href(
                "../foo/bar.txt",
                "file:///root/folder/file.txt"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("../foo/bar.txt", "file:///root/folder/").absoluteHref(percentEncoded = true)
        )

        assertEquals("/bar.txt", Href("foo/../bar.txt", "").absoluteHref(percentEncoded = true))
        assertEquals("/bar.txt", Href("foo/../bar.txt", "/").absoluteHref(percentEncoded = true))
        assertEquals(
            "/bar.txt",
            Href("foo/../bar.txt", "/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/bar.txt",
            Href("foo/../bar.txt", "/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/folder/bar.txt",
            Href("foo/../bar.txt", "/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href(
                "foo/../bar.txt",
                "http://example.com/folder/file.txt"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/folder/bar.txt",
            Href("foo/../bar.txt", "http://example.com/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder/file.txt/bar.txt",
            Href(
                "foo/../bar.txt",
                "file:///root/folder/file.txt"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder/bar.txt",
            Href("foo/../bar.txt", "file:///root/folder/").absoluteHref(percentEncoded = true)
        )

        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt",
            Href("http://absolute.com/foo/bar.txt", "https://example.com/").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/foo/bar.txt",
            Href("file:///root/foo/bar.txt", "file:///root/").absoluteHref(percentEncoded = true)
        )

        // Anchor and query parameters are preserved
        assertEquals(
            "/foo/bar.txt#anchor",
            Href("foo/bar.txt#anchor", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href(
                "file:///root/foo/bar.txt?query=param#anchor",
                "/"
            ).absoluteHref(percentEncoded = true)
        )

        assertEquals(
            "/foo/bar.txt#anchor",
            Href("foo/bar.txt#anchor", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("foo/bar.txt?query=param#anchor", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo/bar.txt?query=param#anchor",
            Href("/foo/bar.txt?query=param#anchor", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://absolute.com/foo/bar.txt?query=param#anchor",
            Href("http://absolute.com/foo/bar.txt?query=param#anchor", "/").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "file:///root/foo/bar.txt?query=param#anchor",
            Href(
                "file:///root/foo/bar.txt?query=param#anchor",
                "/"
            ).absoluteHref(percentEncoded = true)
        )

        // HREF that is just an anchor
        assertEquals("/#anchor", Href("#anchor", "").absoluteHref(percentEncoded = true))
        assertEquals("/#anchor", Href("#anchor", "/").absoluteHref(percentEncoded = true))
        assertEquals(
            "/file.txt#anchor",
            Href("#anchor", "/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/folder#anchor",
            Href("#anchor", "/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/folder/#anchor",
            Href("#anchor", "/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/folder/file.txt#anchor",
            Href(
                "#anchor",
                "http://example.com/folder/file.txt"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/folder#anchor",
            Href("#anchor", "http://example.com/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/folder/#anchor",
            Href("#anchor", "http://example.com/folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder/file.txt#anchor",
            Href("#anchor", "file:///root/folder/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder#anchor",
            Href("#anchor", "file:///root/folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/folder/#anchor",
            Href("#anchor", "file:///root/folder/").absoluteHref(percentEncoded = true)
        )

        // HREF containing spaces.
        assertEquals("/foo%20bar.txt", Href("foo bar.txt", "").absoluteHref(percentEncoded = true))
        assertEquals("/foo%20bar.txt", Href("foo bar.txt", "/").absoluteHref(percentEncoded = true))
        assertEquals(
            "/foo%20bar.txt",
            Href("foo bar.txt", "/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo%20bar.txt",
            Href("foo bar.txt", "/base folder").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "/base folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "/base folder/file.txt").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/base%20folder/foo%20bar.txt",
            Href("foo bar.txt", "base folder/file.txt").absoluteHref(percentEncoded = true)
        )

        // HREF containing special characters
        assertEquals(
            "/base%25folder/foo%20bar/baz%25qux.txt",
            Href("foo bar/baz%qux.txt", "/base%folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/base%20folder/foo%20bar/baz%25qux.txt",
            Href("foo%20bar/baz%25qux.txt", "/base%20folder/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo%20bar/baz%20qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "http://example.com/base%20folder/foo%20bar/baz%20qux.txt",
            Href("foo bar/baz qux.txt", "http://example.com/base%20folder/").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "http://example.com/base%20folder/foo%20bar/baz%25qux.txt",
            Href("foo bar/baz%qux.txt", "http://example.com/base%20folder/").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%20qux.txt",
            Href(
                "foo bar/baz qux.txt",
                "file:///root/base%20folder"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%20qux.txt",
            Href(
                "foo bar/baz qux.txt",
                "file:///root/base%20folder/"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/base%20folder/foo%20bar/baz%25qux.txt",
            Href(
                "foo bar/baz%qux.txt",
                "file:///root/base%20folder/"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "/foo%20bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "/").absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://example.com/foo%20bar.txt?query=param#anchor",
            Href("/foo bar.txt?query=param#anchor", "http://example.com/").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "http://example.com/foo%20bar.txt?query=param#anchor",
            Href("/foo%20bar.txt?query=param#anchor", "http://example.com/").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href(
                "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "http://absolute.com/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href("http://absolute.com/foo bar.txt?query=param#Hello world £500", "/").absoluteHref(
                percentEncoded = true
            )
        )
        assertEquals(
            "file:///foo%20bar.txt?query=param#anchor",
            Href(
                "/foo bar.txt?query=param#anchor",
                "file:///root/"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///foo%20bar.txt?query=param#anchor",
            Href(
                "/foo%20bar.txt?query=param#anchor",
                "file:///root/"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href(
                "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
                "/"
            ).absoluteHref(percentEncoded = true)
        )
        assertEquals(
            "file:///root/foo%20bar.txt?query=param#Hello%20world%20%C2%A3500",
            Href("file:///root/foo bar.txt?query=param#Hello world £500", "/").absoluteHref(
                percentEncoded = true
            )
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
