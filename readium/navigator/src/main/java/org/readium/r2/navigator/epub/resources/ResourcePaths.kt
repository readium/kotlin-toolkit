/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.resources

import android.content.res.Resources
import android.util.TypedValue
import androidx.webkit.WebViewAssetLoader
import java.io.File
import org.readium.r2.shared.util.Url

internal const val ANDROID_RESOURCES_PATH_PREFIX = "android_res"

/**
 * Converts an Android resource id into a relative URL understood by
 * [WebViewAssetLoader.ResourcesPathHandler].
 *
 * The generated URL follows `android_res/<type>/<name>`
 */
internal fun Resources.resourceUrl(resourceId: Int): Url? {
    try {
        val type = getResourceTypeName(resourceId)
        val name = TypedValue().also { getValue(resourceId, it, true) }
            // Preserve the packaged file name when available
            .string?.let { File(it.toString()) }?.name
            ?: getResourceEntryName(resourceId)

        return Url.fromDecodedPath("$ANDROID_RESOURCES_PATH_PREFIX/$type/$name")
    } catch (_: Resources.NotFoundException) {
        return null
    }
}
