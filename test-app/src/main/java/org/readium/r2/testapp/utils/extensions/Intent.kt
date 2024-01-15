/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat

/**
 * Start a new activity to share the given plain [text] to other applications.
 */
fun createShareIntent(
    launchingContext: Context,
    text: String,
    title: String? = null,
): Intent {
    val intent =
        ShareCompat.IntentBuilder(launchingContext)
            .setType("text/plain")
            .setText(text)
            .intent
            .setAction(Intent.ACTION_SEND)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return Intent.createChooser(intent, title)
}
