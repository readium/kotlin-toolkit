/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import android.content.ContentResolver
import android.net.Uri

internal fun ContentResolver.queryProjection(uri: Uri, projection: String): String? =
    query(uri, arrayOf(projection), null, null, null)?.use { cursor ->
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        } catch (e: Exception) {}

        return null
    }
