@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Url.Query
import org.readium.r2.shared.util.Url.QueryParameter

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
        assertEquals(RelativeUrl("/foo/bar")!!, Url("/foo/bar"))
        assertEquals(RelativeUrl("foo/bar")!!, Url("foo/bar"))
        assertEquals(RelativeUrl("../bar")!!, Url("../bar"))

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
    fun createFromFragmentOnly() {
        assertEquals(RelativeUrl("#fragment")!!, Url("#fragment"))
    }

    @Test
    fun createFromQueryOnly() {
        assertEquals(RelativeUrl("?query=param")!!, Url("?query=param"))
    }

    @Test
    fun createFromAbsoluteUrl() {
        assertEquals(
            AbsoluteUrl("http://example.com/foo")!!,
            Url("http://example.com/foo")
        )
        assertEquals(AbsoluteUrl("file:///foo/bar")!!, Url("file:///foo/bar"))
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
    fun queryParametersAreOnlySeparatedByAmpersands() {
        // Unlike `android.net.UrlQuerySanitizer`, `;` is not treated as a parameter separator,
        // consistently with the WHATWG URL specification.
        assertEquals(
            Query(listOf(QueryParameter(name = "query", value = "param;fruit=banana"))),
            Url("http://domain.com/path?query=param;fruit=banana")!!.query
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
    fun resolveQueryOnlyReference() {
        // Unlike `java.net.URI`, a query-only reference keeps the whole base path, per
        // RFC 3986 §5.2.2 and consistently with the WHATWG URL specification.
        // `java.net.URI` would resolve against the parent directory instead
        // ("http://example.com/foo/?query=param").
        assertEquals(
            Url("http://example.com/foo/bar?query=param")!!,
            Url("http://example.com/foo/bar")!!.resolve(Url("?query=param")!!)
        )
        assertEquals(
            Url("foo/bar?query=param")!!,
            Url("foo/bar")!!.resolve(Url("?query=param")!!)
        )
    }

    @Test
    fun resolveRemovesDotSegmentsOfAbsolutePathReference() {
        // Unlike `java.net.URI`, the dot segments of an absolute-path reference are removed, per
        // RFC 3986 §5.2.2 and consistently with the WHATWG URL specification.
        // `java.net.URI` would keep them literally ("http://example.com/quz/../baz").
        val base = Url("http://example.com/foo/bar")!!
        assertEquals(Url("http://example.com/baz")!!, base.resolve(Url("/quz/../baz")!!))
        assertEquals(Url("http://example.com/quz/")!!, base.resolve(Url("/quz/baz/..")!!))
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
        assertEquals(params.allNamed("empty"), emptyList())
        assertEquals(params.allNamed("not-found"), emptyList())
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

    /**
     * Characterization tests capturing the behavior of the historical implementation based on
     * `java.io.File.normalize()`, before it was reimplemented in pure Kotlin.
     */
    @Test
    fun normalizePathCharacterization() {
        fun test(expected: String, url: String) {
            assertEquals(expected, Url(url)!!.normalize().toString())
        }

        // `.` and `..` collapsing.
        test("foo/baz", "foo/./bar/../baz")
        test("http://example.com/foo/baz", "http://example.com/foo/./bar/../baz")
        test("foo", "./foo")
        test("foo", "foo/.")
        test("foo/", "foo/./")

        // Leading `..` segments are retained.
        test("../../foo", "../../foo")
        test("../baz", "foo/./bar/../../../baz")
        test("..", "foo/../..")
        test("../", "foo/../../")
        test("http://example.com/../foo", "http://example.com/../foo")

        // Trailing `..` collapses the parent directory.
        test("foo", "foo/bar/..")
        test("foo/", "foo/bar/../")

        // Duplicate separators are collapsed.
        test("foo/bar", "foo//bar")
        test("foo/bar/", "foo//bar//")
        test("http://example.com/foo/bar", "http://example.com//foo//bar")

        // Path reduced to nothing.
        test("", ".")
        test("", "foo/..")
        test("/", "foo/../")

        // Root paths.
        test("http://example.com", "http://example.com")
        test("http://example.com//", "http://example.com/")
    }
}
