/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.demo.navigator.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.browser.customtabs.CustomTabsIntent
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog

/**
 * Opens the given [uri] with a Chrome Custom Tab or the system browser as a fallback.
 */
@OptIn(InternalReadiumApi::class)
fun launchWebBrowser(context: Context, uri: Uri) {
    var url = uri
    if (url.scheme == null) {
        url = url.buildUpon().scheme("http").build()
    }

    if (!URLUtil.isNetworkUrl(url.toString())) {
        return
    }

    tryOrLog {
        try {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, url)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
        }
    }
}
