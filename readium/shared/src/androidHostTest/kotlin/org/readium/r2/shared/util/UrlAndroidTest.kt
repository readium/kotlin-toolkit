package org.readium.r2.shared.util

import java.io.File
import java.net.URI
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlAndroidTest {

    @Test
    fun fromFile() {
        assertEquals(
            AbsoluteUrl("file:///tmp/test.txt"),
            File("/tmp/test.txt").toUrl(isDirectory = false)
        )
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
        assertEquals(AbsoluteUrl("file:///tmp/"), File("/tmp").toUrl(isDirectory = true))
    }

    @Test
    fun fromAndroidUri() {
        assertEquals(RelativeUrl("foo/bar"), android.net.Uri.parse("foo/bar").toUrl())
        assertEquals(
            AbsoluteUrl("http://example.com/foo/bar"),
            android.net.Uri.parse("http://example.com/foo/bar").toAbsoluteUrl()
        )
    }

    @Test
    fun toAndroidUri() {
        assertEquals(
            android.net.Uri.parse("http://example.com/foo/bar"),
            Url("http://example.com/foo/bar")!!.toUri()
        )
    }

    @Test
    fun fromURI() {
        assertEquals(RelativeUrl("foo/bar"), URI("foo/bar").toUrl())
        assertEquals(RelativeUrl("/foo/bar"), URI("/foo/bar").toUrl())
        assertEquals(
            AbsoluteUrl("http://example.com/foo/bar"),
            URI("http://example.com/foo/bar").toUrl()
        )
        assertEquals(
            AbsoluteUrl("file:///tmp/test.txt"),
            URI("file:///tmp/test.txt").toUrl()
        )
        assertEquals(
            AbsoluteUrl("file:///tmp/test.txt"),
            URI("file:/tmp/test.txt").toUrl()
        )
    }

    @Test
    fun fromURL() {
        assertEquals(
            AbsoluteUrl("http://example.com/foo/bar"),
            URL("http://example.com/foo/bar").toUrl()
        )
        assertEquals(
            AbsoluteUrl("file:///tmp/test.txt"),
            URL("file:///tmp/test.txt").toUrl()
        )
        assertEquals(
            AbsoluteUrl("file:///tmp/test.txt"),
            URL("file:/tmp/test.txt").toUrl()
        )
    }
}
