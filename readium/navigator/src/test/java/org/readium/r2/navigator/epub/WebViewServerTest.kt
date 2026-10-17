/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import android.net.Uri
import android.webkit.WebResourceRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalReadiumApi::class)
class WebViewServerTest {

    @Test
    fun `missing allowed asset returns an empty response with CORS`() {
        val assetPath = "missing-reader-helper.js"
        val server = WebViewServer(
            application = RuntimeEnvironment.getApplication(),
            publication = Publication(Manifest(metadata = Metadata())),
            servedAssets = listOf(assetPath),
            disableSelectionWhenProtected = false,
            onResourceLoadFailed = { _, _ -> error("Expected an allowed asset request") }
        )
        val request = object : WebResourceRequest {
            override fun getUrl(): Uri = Uri.parse("https://${WebViewServer.ASSETS_HOSTNAME}/$assetPath")
            override fun isForMainFrame(): Boolean = false
            override fun isRedirect(): Boolean = false
            override fun hasGesture(): Boolean = false
            override fun getMethod(): String = "GET"
            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }

        val response = assertNotNull(
            server.shouldInterceptRequest(
                request,
                ReadiumCss(assetsBaseHref = WebViewServer.assetsBaseHref)
            )
        )

        assertNull(response.data)
        assertEquals("*", response.responseHeaders["Access-Control-Allow-Origin"])
    }
}
