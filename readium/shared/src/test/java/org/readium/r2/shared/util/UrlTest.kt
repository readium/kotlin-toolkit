package org.readium.r2.shared.util

import android.net.Uri
import java.io.File
import java.net.URI
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
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

        assertNull(AbsoluteUrl("  "))
        assertNull(AbsoluteUrl("invalid character"))

        assertNull(RelativeUrl("   "))
        assertNull(RelativeUrl("invalid character"))
    }

    @Test
    fun createFromRelativePath() {
        assertEquals(RelativeUrl(Uri.parse("/foo/bar")), Url("/foo/bar"))
        assertEquals(RelativeUrl(Uri.parse("foo/bar")), Url("foo/bar"))
        assertEquals(RelativeUrl(Uri.parse("../bar")), Url("../bar"))

        // Used in the EPUB parser
        assertEquals(RelativeUrl(Uri.parse("#")), Url("#"))
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
        assertEquals("txt", Url("foo/bar.txt?query#fragment")?.extension)
        assertEquals(null, Url("foo/bar?query#fragment")?.extension)
        assertEquals(null, Url("foo/bar/?query#fragment")?.extension)
        assertEquals("txt", Url("http://example.com/foo/bar.txt?query#fragment")?.extension)
        assertEquals(null, Url("http://example.com/foo/bar?query#fragment")?.extension)
        assertEquals(null, Url("http://example.com/foo/bar/")?.extension)
        assertEquals("txt", Url("file:///foo/bar.txt?query#fragment")?.extension)
        assertEquals(null, Url("file:///foo/bar?query#fragment")?.extension)
        assertEquals(null, Url("file:///foo/bar/")?.extension)
    }

    @Test
    fun extensionIsPercentDecoded() {
        assertEquals("%bar", Url("foo.%25bar")?.extension)
        assertEquals("%bar", Url("http://example.com/foo.%25bar")?.extension)
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
        assertEquals(AbsoluteUrl(Uri.parse("file:///tmp/test.txt")), File("/tmp/test.txt").toUrl())
    }

    @Test
    fun toFile() {
        assertEquals(
            File("/tmp/test.txt"),
            (Url("file:///tmp/test.txt") as? AbsoluteUrl)?.toFile()
        )
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
}
