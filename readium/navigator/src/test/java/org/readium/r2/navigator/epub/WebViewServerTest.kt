/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import android.net.Uri
import android.webkit.WebResourceRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.R
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WebViewServerTest {

    @Test
    fun `Reject an asset allowed only as an Android resource`() {
        val path = "drawable/readium_media_notification_fastforward.xml"
        val failures = mutableListOf<Url>()
        val server = server(
            servedAndroidResources = listOf(R.drawable.readium_media_notification_fastforward),
            failures = failures
        )

        assertNotNull(server.shouldInterceptRequest(request(WebViewServer.assetsBaseHref, path), css()))
        assertEquals(AbsoluteUrl("${WebViewServer.assetsBaseHref}$path")!!, failures.single())
    }

    @Test
    fun `Reject an Android resource allowed only as an asset`() {
        val path = "drawable/readium_media_notification_fastforward.xml"
        val failures = mutableListOf<Url>()
        val server = server(servedAssets = listOf(path), failures = failures)

        assertNotNull(server.shouldInterceptRequest(request(WebViewServer.resourcesBaseHref, path), css()))
        assertEquals(AbsoluteUrl("${WebViewServer.resourcesBaseHref}$path")!!, failures.single())
    }

    @Test
    fun `Serve an allowed asset with CORS`() {
        val path = "readium/fonts/OpenDyslexic-Regular.otf"
        val failures = mutableListOf<Url>()
        val server = server(servedAssets = listOf(path), failures = failures)

        val response = assertNotNull(server.shouldInterceptRequest(request(WebViewServer.assetsBaseHref, path), css()))

        assertTrue(failures.isEmpty())
        assertEquals(200, response.statusCode)
        assertEquals("*", response.responseHeaders["Access-Control-Allow-Origin"])
    }

    @Test
    fun `Serve an allowed Android resource with CORS`() {
        val path = "drawable/readium_media_notification_fastforward.xml"
        val failures = mutableListOf<Url>()
        val server = server(
            servedAndroidResources = listOf(R.drawable.readium_media_notification_fastforward),
            failures = failures
        )

        val response = assertNotNull(server.shouldInterceptRequest(request(WebViewServer.resourcesBaseHref, path), css()))

        assertTrue(failures.isEmpty())
        assertEquals(200, response.statusCode)
        assertEquals("*", response.responseHeaders["Access-Control-Allow-Origin"])
    }

    @Test
    fun `Reject an Android resource with an unlisted ID`() {
        val path = "drawable/readium_media_notification_rewind.xml"
        val failures = mutableListOf<Url>()
        val server = server(
            servedAndroidResources = listOf(R.drawable.readium_media_notification_fastforward),
            failures = failures
        )

        assertNotNull(server.shouldInterceptRequest(request(WebViewServer.resourcesBaseHref, path), css()))
        assertEquals(AbsoluteUrl("${WebViewServer.resourcesBaseHref}$path")!!, failures.single())
    }

    @Test
    fun `Ignore a non-file resource ID when serving resources`() {
        val path = "drawable/readium_media_notification_fastforward.xml"
        val failures = mutableListOf<Url>()
        val server = server(
            servedAndroidResources = listOf(R.string.readium_media_notification_channel_name),
            failures = failures
        )

        assertNotNull(server.shouldInterceptRequest(request(WebViewServer.resourcesBaseHref, path), css()))
        assertEquals(AbsoluteUrl("${WebViewServer.resourcesBaseHref}$path")!!, failures.single())
    }

    private fun server(
        servedAssets: List<String> = emptyList(),
        servedAndroidResources: List<Int> = emptyList(),
        failures: MutableList<Url>,
    ): WebViewServer =
        WebViewServer(
            application = RuntimeEnvironment.getApplication(),
            publication = Publication(Manifest(metadata = Metadata())),
            servedAssets = servedAssets,
            servedAndroidResources = servedAndroidResources,
            disableSelectionWhenProtected = false,
            onResourceLoadFailed = { url, _ -> failures.add(url) }
        )

    private fun css(): ReadiumCss = ReadiumCss(assetsBaseHref = WebViewServer.assetsBaseHref)

    private fun request(baseHref: AbsoluteUrl, path: String): WebResourceRequest =
        object : WebResourceRequest {
            override fun getUrl(): Uri = Uri.parse("$baseHref$path")
            override fun isForMainFrame(): Boolean = false
            override fun isRedirect(): Boolean = false
            override fun hasGesture(): Boolean = false
            override fun getMethod(): String = "GET"
            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }
}
