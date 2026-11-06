/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.R
import org.readium.r2.shared.util.AbsoluteUrl
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidResourceUrlTest {

    @Test
    fun `Create separate URLs for Android resources and app assets`() {
        val resources = RuntimeEnvironment.getApplication().resources

        assertEquals(
            AbsoluteUrl("${WebViewServer.resourcesBaseHref}drawable/readium_media_notification_fastforward.xml")!!,
            EpubNavigatorFragment.androidResourceUrl(
                resources,
                R.drawable.readium_media_notification_fastforward
            )
        )

        assertEquals(
            AbsoluteUrl("${WebViewServer.assetsBaseHref}android_res/drawable/readium_media_notification_fastforward.xml")!!,
            EpubNavigatorFragment.assetUrl("android_res/drawable/readium_media_notification_fastforward.xml")
        )
    }

    @Test
    fun `Return null for an unknown Android resource`() {
        val resources = RuntimeEnvironment.getApplication().resources

        assertNull(EpubNavigatorFragment.androidResourceUrl(resources, 0))
    }

    @Test
    fun `Return null for a string resource`() {
        val resources = RuntimeEnvironment.getApplication().resources

        assertNull(
            EpubNavigatorFragment.androidResourceUrl(
                resources,
                R.string.readium_media_notification_channel_name
            )
        )
    }

    @Test
    fun `Create a server URL for an Android file resource`() {
        val resources = RuntimeEnvironment.getApplication().resources

        assertEquals(
            AbsoluteUrl("${WebViewServer.resourcesBaseHref}drawable/readium_media_notification_fastforward.xml")!!,
            WebViewServer.androidResourceUrl(
                resources,
                R.drawable.readium_media_notification_fastforward
            )
        )
    }

    @Test
    fun `Return null for an unknown server resource`() {
        val resources = RuntimeEnvironment.getApplication().resources

        assertNull(WebViewServer.androidResourceUrl(resources, 0))
    }
}
