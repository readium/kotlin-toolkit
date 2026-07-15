@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util

import android.net.Uri
import java.io.File
import java.net.URI
import java.net.URL
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Url.Query
import org.readium.r2.shared.util.Url.QueryParameter
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlTest {

    @Test
    fun createFromInvalidUrl() {
        assertNull(Url(""))
        assertNull(Url("     "))
        assertNull(Url("invalid character"))
        assertNull(Url("école"))

        assertNull(AbsoluteUrl("  "))
        assertNull(AbsoluteUrl("invalid character"))
        assertNull(AbsoluteUrl("école"))

        assertNull(RelativeUrl("   "))
        assertNull(RelativeUrl("invalid character"))
        assertNull(RelativeUrl("école"))
    }

    @Test
    fun createFromRelativePath() {
        assertEquals(RelativeUrl(Uri.parse("/foo/bar")), Url("/foo/bar"))
        assertEquals(RelativeUrl(Uri.parse("foo/bar")), Url("foo/bar"))
        assertEquals(RelativeUrl(Uri.parse("../bar")), Url("../bar"))

        // Special characters valid in a path.
        assertEquals("$&+,/=@", RelativeUrl("$&+,/=@")?.path)

        // Used in the EPUB parser
        val url = Url("#") as? RelativeUrl
        assertNotNull(url)
        assertEquals(null, url.path)
        assertEquals(null, url.fragment)
    }

    @OptIn(DelicateReadiumApi::class)
    @Test
    fun createFromLegacyHref() {
        testLegacy<RelativeUrl>("dir/chapter.xhtml", "dir/chapter.xhtml")
        // Starting slash is removed.
        testLegacy<RelativeUrl>("/dir/chapter.xhtml", "dir/chapter.xhtml")
        // Special characters are percent-encoded.
        testLegacy<RelativeUrl>("/dir/per%cent.xhtml", "dir/per%25cent.xhtml")
        testLegacy<RelativeUrl>("/barré.xhtml", "barr%C3%A9.xhtml")
        testLegacy<RelativeUrl>("/spa ce.xhtml", "spa%20ce.xhtml")
        // We assume that a relative path is percent-decoded.
        testLegacy<RelativeUrl>("/spa%20ce.xhtml", "spa%2520ce.xhtml")
        // Some special characters are authorized in a path.
        testLegacy<RelativeUrl>("/$&+,/=@", "$&+,/=@")
        // Valid absolute URL are left untouched.
        testLegacy<AbsoluteUrl>(
            "http://domain.com/a%20book?page=3",
            "http://domain.com/a%20book?page=3"
        )
        // Invalid absolute URL.
        assertNull(Url.fromLegacyHref("http://domain.com/a book"))
    }

    @OptIn(DelicateReadiumApi::class)
    private inline fun <reified T : Url> testLegacy(href: String, expected: String) {
        val url = Url.fromLegacyHref(href)
        assertNotNull(url)
        assertIs<T>(url)
        assertEquals(expected, url.toString())
    }

    @Test
    fun createFromEpubHref() {
        fun testEpub(href: String, expected: String) {
            val url = Url.fromEpubHref(href)
            assertNotNull(url)
            assertEquals(expected, url.toString())
        }

        // Valid percent-encoded HREFs are used as-is.
        testEpub("dir/chapter.xhtml", "dir/chapter.xhtml")
        testEpub("dir/chapter.xhtml#frag", "dir/chapter.xhtml#frag")
        testEpub("dir/chapter.xhtml?q=1#frag", "dir/chapter.xhtml?q=1#frag")
        // Already-encoded characters are not re-encoded.
        testEpub("dir/chapter%20one.xhtml#frag", "dir/chapter%20one.xhtml#frag")

        // Decoded paths without separators are percent-encoded.
        testEpub("dir/chapter one.xhtml", "dir/chapter%20one.xhtml")
        testEpub("/dir/chapter one.xhtml", "/dir/chapter%20one.xhtml")

        // When the path is not percent-encoded, only the path is encoded while the `?query` and
        // `#fragment` — and their separators — are preserved verbatim.
        testEpub(
            "../content/Harry Potter 1.xhtml#fragment-01",
            "../content/Harry%20Potter%201.xhtml#fragment-01"
        )
        testEpub("/dir/my file.xhtml#frag", "/dir/my%20file.xhtml#frag")
        testEpub("dir/my file.xhtml?q=1", "dir/my%20file.xhtml?q=1")
        testEpub("dir/my file.xhtml?q=1#frag", "dir/my%20file.xhtml?q=1#frag")

        // An already-encoded fragment is not double-encoded.
        testEpub("hello world#properly%20encoded", "hello%20world#properly%20encoded")

        // A question mark inside the fragment is kept in the fragment.
        testEpub("dir/a b.xhtml#f?x", "dir/a%20b.xhtml#f?x")

        // Media Overlays clip URL (from the SMIL parser): the comma is valid in a fragment and is
        // kept, while the space in the path is encoded.
        testEpub("audio/my file.mp3#t=0.5,10.2", "audio/my%20file.mp3#t=0.5,10.2")

        // A raw space in the fragment or query is invalid for Java's URI parser, so reassembly fails
        // and we fall back to encoding the invalid characters of the query/fragment, following the
        // same policy as the path.
        testEpub("chapter one.xhtml#foo bar", "chapter%20one.xhtml#foo%20bar")
        testEpub("chapter one.xhtml?foo bar", "chapter%20one.xhtml?foo%20bar")
        testEpub("chapter one.xhtml?a b#c d", "chapter%20one.xhtml?a%20b#c%20d")
        // The query structure (`&` and `=`) is preserved while invalid characters are encoded.
        testEpub("my file.xhtml?a=b c&d=e", "my%20file.xhtml?a=b%20c&d=e")
        // A partially-encoded query or fragment keeps its existing percent escapes instead of
        // double-encoding them (`%20` must stay `%20`, not become `%2520`) when a raw space forces
        // the fallback encoding.
        testEpub("my file.xhtml#foo%20bar baz", "my%20file.xhtml#foo%20bar%20baz")
        testEpub("my file.xhtml?a=b%20c&d=e f", "my%20file.xhtml?a=b%20c&d=e%20f")

        // Returns null for an empty HREF.
        assertNull(Url.fromEpubHref(""))
    }

    @Test
    fun createFromFragmentOnly() {
        assertEquals(RelativeUrl(Uri.parse("#fragment")), Url("#fragment"))
    }

    @Test
    fun createFromQueryOnly() {
        assertEquals(RelativeUrl(Uri.parse("?query=param")), Url("?query=param"))
    }

    @Test
    fun createFromAbsoluteUrl() {
        assertEquals(
            AbsoluteUrl(Uri.parse("http://example.com/foo")),
            Url("http://example.com/foo")
        )
        assertEquals(AbsoluteUrl(Uri.parse("file:///foo/bar")), Url("file:///foo/bar"))
    }

    @Test
    fun getToString() {
        assertEquals("foo/bar?query#fragment", Url("foo/bar?query#fragment")?.toString())
        assertEquals(
            "http://example.com/foo/bar?query#fragment",
            Url("http://example.com/foo/bar?query#fragment")?.toString()
        )
        assertEquals(
            "file:///foo/bar?query#fragment",
            Url("file:///foo/bar?query#fragment")?.toString()
        )
    }

    @Test
    fun getPath() {
        assertEquals("foo/bar", RelativeUrl("foo/bar?query#fragment")?.path)
        assertEquals("/foo/bar/", AbsoluteUrl("http://example.com/foo/bar/")?.path)
        assertEquals("/foo/bar", AbsoluteUrl("http://example.com/foo/bar?query#fragment")?.path)
        assertEquals("/foo/bar/", AbsoluteUrl("file:///foo/bar/")?.path)
        assertEquals("/foo/bar", AbsoluteUrl("file:///foo/bar?query#fragment")?.path)
    }

    @Test
    fun getPathFromEmptyRelativeUrl() {
        assertNull(RelativeUrl("#fragment")!!.path)
    }

    @Test
    fun pathIsPercentDecoded() {
        assertEquals("foo/%bar quz", Url("foo/%25bar%20quz")?.path)
        assertEquals("/foo/%bar quz", Url("http://example.com/foo/%25bar%20quz")?.path)
    }

    @Test
    fun getFilename() {
        assertEquals("bar", Url("foo/bar?query#fragment")?.filename)
        assertEquals(null, Url("foo/bar/?query#fragment")?.filename)
        assertEquals("bar", Url("http://example.com/foo/bar?query#fragment")?.filename)
        assertEquals(null, Url("http://example.com/foo/bar/")?.filename)
        assertEquals("bar", Url("file:///foo/bar?query#fragment")?.filename)
        assertEquals(null, Url("file:///foo/bar/")?.filename)
    }

    @Test
    fun filenameIsPercentDecoded() {
        assertEquals("%bar quz", Url("foo/%25bar%20quz")?.filename)
        assertEquals("%bar quz", Url("http://example.com/foo/%25bar%20quz")?.filename)
    }

    @Test
    fun getExtension() {
        assertEquals("txt", Url("foo/bar.txt?query#fragment")?.extension?.value)
        assertEquals(null, Url("foo/bar?query#fragment")?.extension)
        assertEquals(null, Url("foo/bar/?query#fragment")?.extension)
        assertEquals("txt", Url("http://example.com/foo/bar.txt?query#fragment")?.extension?.value)
        assertEquals(null, Url("http://example.com/foo/bar?query#fragment")?.extension)
        assertEquals(null, Url("http://example.com/foo/bar/")?.extension)
        assertEquals("txt", Url("file:///foo/bar.txt?query#fragment")?.extension?.value)
        assertEquals(null, Url("file:///foo/bar?query#fragment")?.extension)
        assertEquals(null, Url("file:///foo/bar/")?.extension)
    }

    @Test
    fun extensionIsPercentDecoded() {
        assertEquals("%bar", Url("foo.%25bar")?.extension?.value)
        assertEquals("%bar", Url("http://example.com/foo.%25bar")?.extension?.value)
    }

    @Test
    fun getQueryParameters() {
        assertEquals(Query(emptyList()), Url("http://domain.com/path")!!.query)
        assertEquals(
            Query(listOf(QueryParameter(name = "query", value = "param"))),
            Url("http://domain.com/path?query=param#anchor")!!.query
        )
        assertEquals(
            Query(
                listOf(
                    QueryParameter(name = "query", value = "param"),
                    QueryParameter(name = "fruit", value = "banana"),
                    QueryParameter(name = "query", value = "other"),
                    QueryParameter(name = "empty", value = null)
                )
            ),
            Url("http://domain.com/path?query=param&fruit=banana&query=other&empty")!!.query
        )
    }

    @Test
    fun getScheme() {
        assertEquals(Url.Scheme("content"), (Url("content:///foo/bar") as? AbsoluteUrl)?.scheme)
        assertEquals(Url.Scheme("content"), (Url("CONTENT:///foo/bar") as? AbsoluteUrl)?.scheme)
        assertEquals(Url.Scheme("file"), (Url("file:///foo/bar") as? AbsoluteUrl)?.scheme)
        assertEquals(Url.Scheme("http"), (Url("http://example.com/foo") as? AbsoluteUrl)?.scheme)
        assertEquals(Url.Scheme("https"), (Url("https://example.com/foo") as? AbsoluteUrl)?.scheme)
    }

    @Test
    fun testScheme() {
        assertEquals(true, (Url("content:///foo/bar") as? AbsoluteUrl)?.isContent)
        assertEquals(false, (Url("content:///foo/bar") as? AbsoluteUrl)?.isHttp)

        assertEquals(true, (Url("file:///foo/bar") as? AbsoluteUrl)?.isFile)
        assertEquals(false, (Url("file:///foo/bar") as? AbsoluteUrl)?.isContent)

        assertEquals(true, (Url("http://example.com/foo") as? AbsoluteUrl)?.isHttp)
        assertEquals(true, (Url("https://example.com/foo") as? AbsoluteUrl)?.isHttp)
        assertEquals(false, (Url("http://example.com/foo") as? AbsoluteUrl)?.isFile)
    }

    @Test
    fun resolveHttpUrl() {
        var base = Url("http://example.com/foo/bar")!!
        assertEquals(Url("http://example.com/foo/quz/baz")!!, base.resolve(Url("quz/baz")!!))
        assertEquals(Url("http://example.com/quz/baz")!!, base.resolve(Url("../quz/baz")!!))
        assertEquals(Url("http://example.com/quz/baz")!!, base.resolve(Url("/quz/baz")!!))
        assertEquals(Url("http://example.com/foo/bar#fragment")!!, base.resolve(Url("#fragment")!!))
        assertEquals(Url("file:///foo/bar")!!, base.resolve(Url("file:///foo/bar")!!))

        // With trailing slash
        base = Url("http://example.com/foo/bar/")!!
        assertEquals(Url("http://example.com/foo/bar/quz/baz")!!, base.resolve(Url("quz/baz")!!))
        assertEquals(Url("http://example.com/foo/quz/baz")!!, base.resolve(Url("../quz/baz")!!))
    }

    @Test
    fun resolveFileUrl() {
        var base = Url("file:///root/foo/bar")!!
        assertEquals(Url("file:///root/foo/quz")!!, base.resolve(Url("quz")!!))
        assertEquals(Url("file:///root/foo/quz/baz")!!, base.resolve(Url("quz/baz")!!))
        assertEquals(Url("file:///root/quz")!!, base.resolve(Url("../quz")!!))
        assertEquals(Url("file:///quz/baz")!!, base.resolve(Url("/quz/baz")!!))
        assertEquals(
            Url("http://example.com/foo/bar")!!,
            base.resolve(Url("http://example.com/foo/bar")!!)
        )

        // With trailing slash
        base = Url("file:///root/foo/bar/")!!
        assertEquals(Url("file:///root/foo/bar/quz/baz")!!, base.resolve(Url("quz/baz")!!))
        assertEquals(Url("file:///root/foo/quz")!!, base.resolve(Url("../quz")!!))
    }

    @Test
    fun resolveTwoRelativeUrls() {
        var base = Url("foo/bar")!!
        assertEquals(Url("foo/quz/baz")!!, base.resolve(Url("quz/baz")!!))
        assertEquals(Url("quz/baz")!!, base.resolve(Url("../quz/baz")!!))
        assertEquals(Url("/quz/baz")!!, base.resolve(Url("/quz/baz")!!))
        assertEquals(Url("foo/bar#fragment")!!, base.resolve(Url("#fragment")!!))
        assertEquals(
            Url("http://example.com/foo/bar")!!,
            base.resolve(Url("http://example.com/foo/bar")!!)
        )

        // With trailing slash
        base = Url("foo/bar/")!!
        assertEquals(Url("foo/bar/quz/baz")!!, base.resolve(Url("quz/baz")!!))
        assertEquals(Url("foo/quz/baz")!!, base.resolve(Url("../quz/baz")!!))

        // With starting slash
        base = Url("/foo/bar")!!
        assertEquals(Url("/foo/quz/baz")!!, base.resolve(Url("quz/baz")!!))
        assertEquals(Url("/quz/baz")!!, base.resolve(Url("/quz/baz")!!))
    }

    @Test
    fun relativizeHttpUrl() {
        var base = Url("http://example.com/foo")!!
        assertEquals(Url("quz/baz")!!, base.relativize(Url("http://example.com/foo/quz/baz")!!))
        assertEquals(Url("#fragment")!!, base.relativize(Url("http://example.com/foo#fragment")!!))
        assertEquals(Url("#fragment")!!, base.relativize(Url("http://example.com/foo/#fragment")!!))
        assertEquals(Url("file:///foo/bar")!!, base.relativize(Url("file:///foo/bar")!!))

        // With trailing slash
        base = Url("http://example.com/foo/")!!
        assertEquals(Url("quz/baz")!!, base.relativize(Url("http://example.com/foo/quz/baz")!!))
    }

    @Test
    fun relativizeFileUrl() {
        var base = Url("file:///root/foo")!!
        assertEquals(Url("quz/baz")!!, base.relativize(Url("file:///root/foo/quz/baz")!!))
        assertEquals(
            Url("http://example.com/foo/bar")!!,
            base.relativize(Url("http://example.com/foo/bar")!!)
        )

        // With trailing slash
        base = Url("file:///root/foo/")!!
        assertEquals(Url("quz/baz")!!, base.relativize(Url("file:///root/foo/quz/baz")!!))
    }

    @Test
    fun relativizeTwoRelativeUrls() {
        var base = Url("foo")!!
        assertEquals(Url("quz/baz")!!, base.relativize(Url("foo/quz/baz")!!))
        assertEquals(Url("quz/baz")!!, base.relativize(Url("quz/baz")!!))
        assertEquals(Url("/quz/baz")!!, base.relativize(Url("/quz/baz")!!))
        assertEquals(Url("#fragment")!!, base.relativize(Url("foo#fragment")!!))
        assertEquals(Url("#fragment")!!, base.relativize(Url("foo/#fragment")!!))
        assertEquals(
            Url("http://example.com/foo/bar")!!,
            base.relativize(Url("http://example.com/foo/bar")!!)
        )

        // With trailing slash
        base = Url("foo/")!!
        assertEquals(Url("quz/baz")!!, base.relativize(Url("foo/quz/baz")!!))

        // With starting slash
        base = Url("/foo")!!
        assertEquals(Url("quz/baz")!!, base.relativize(Url("/foo/quz/baz")!!))
        assertEquals(Url("/quz/baz")!!, base.relativize(Url("/quz/baz")!!))
    }

    @Test
    fun fromFile() {
        assertEquals(AbsoluteUrl(Uri.parse("file:///tmp/test.txt")), File("/tmp/test.txt").toUrl(isDirectory = false))
    }

    @Test
    fun toFile() {
        assertEquals(
            File("/tmp/test.txt"),
            (Url("file:///tmp/test.txt") as? AbsoluteUrl)?.toFile()
        )
    }

    @Test
    fun fromDirectory() {
        assertEquals(AbsoluteUrl(Uri.parse("file:///tmp/")), File("/tmp").toUrl(isDirectory = true))
    }

    @Test
    fun fromURI() {
        assertEquals(RelativeUrl(Uri.parse("foo/bar")), URI("foo/bar").toUrl())
        assertEquals(RelativeUrl(Uri.parse("/foo/bar")), URI("/foo/bar").toUrl())
        assertEquals(
            AbsoluteUrl(Uri.parse("http://example.com/foo/bar")),
            URI("http://example.com/foo/bar").toUrl()
        )
        assertEquals(
            AbsoluteUrl(Uri.parse("file:///tmp/test.txt")),
            URI("file:///tmp/test.txt").toUrl()
        )
        assertEquals(
            AbsoluteUrl(Uri.parse("file:///tmp/test.txt")),
            URI("file:/tmp/test.txt").toUrl()
        )
    }

    @Test
    fun fromURL() {
        assertEquals(
            AbsoluteUrl(Uri.parse("http://example.com/foo/bar")),
            URL("http://example.com/foo/bar").toUrl()
        )
        assertEquals(
            AbsoluteUrl(Uri.parse("file:///tmp/test.txt")),
            URL("file:///tmp/test.txt").toUrl()
        )
        assertEquals(
            AbsoluteUrl(Uri.parse("file:///tmp/test.txt")),
            URL("file:/tmp/test.txt").toUrl()
        )
    }

    @Test
    fun getFirstParameterNamedX() {
        val params = Query(
            listOf(
                QueryParameter(name = "query", value = "param"),
                QueryParameter(name = "fruit", value = "banana"),
                QueryParameter(name = "query", value = "other"),
                QueryParameter(name = "empty", value = null)
            )
        )

        assertEquals(params.firstNamedOrNull("query"), "param")
        assertEquals(params.firstNamedOrNull("fruit"), "banana")
        assertNull(params.firstNamedOrNull("empty"))
        assertNull(params.firstNamedOrNull("not-found"))
    }

    @Test
    fun getAllParametersNamedX() {
        val params = Query(
            listOf(
                QueryParameter(name = "query", value = "param"),
                QueryParameter(name = "fruit", value = "banana"),
                QueryParameter(name = "query", value = "other"),
                QueryParameter(name = "empty", value = null)
            )
        )

        assertEquals(params.allNamed("query"), listOf("param", "other"))
        assertEquals(params.allNamed("fruit"), listOf("banana"))
        assertEquals(params.allNamed("empty"), emptyList<String>())
        assertEquals(params.allNamed("not-found"), emptyList<String>())
    }

    @Test
    fun normalize() {
        // Scheme is lower case.
        assertEquals(
            "http://example.com",
            Url("HTTP://example.com")!!.normalize().toString()
        )

        // Percent encoding of path is normalized.
        assertEquals(
            "http://example.com/c'est%20valide",
            Url("http://example.com/c%27est%20valide")!!.normalize().toString()
        )
        assertEquals(
            "c'est%20valide",
            Url("c%27est%20valide")!!.normalize().toString()
        )

        // Relative paths are resolved.
        assertEquals(
            "http://example.com/foo/baz",
            Url("http://example.com/foo/./bar//../baz")!!.normalize().toString()
        )
        assertEquals(
            "foo/baz",
            Url("foo/./bar//../baz")!!.normalize().toString()
        )
        assertEquals(
            "../baz",
            Url("foo/./bar/../../../baz")!!.normalize().toString()
        )

        // Trailing slash is kept.
        assertEquals(
            "http://example.com/foo/",
            Url("http://example.com/foo/")!!.normalize().toString()
        )
        assertEquals(
            "foo/",
            Url("foo/")!!.normalize().toString()
        )

        // The other components are left as-is.
        assertEquals(
            "http://user:password@example.com:443/foo?b=b&a=a#fragment",
            Url("http://user:password@example.com:443/foo?b=b&a=a#fragment")!!.normalize().toString()
        )
    }
}
