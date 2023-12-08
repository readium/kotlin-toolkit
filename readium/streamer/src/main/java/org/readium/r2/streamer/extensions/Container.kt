/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import java.io.File
import org.readium.r2.shared.util.Url

internal fun Iterable<Url>.guessTitle(): String? {
    val firstEntry = firstOrNull() ?: return null
    val commonFirstComponent = pathCommonFirstComponent() ?: return null

    if (commonFirstComponent.name == firstEntry.path) {
        return null
    }

    return commonFirstComponent.name
}

/** Returns a [File] to the directory containing all paths, if there is such a directory. */
internal fun Iterable<Url>.pathCommonFirstComponent(): File? =
    mapNotNull { it.path?.substringBefore("/") }
        .distinct()
        .takeIf { it.size == 1 }
        ?.firstOrNull()
        ?.let { File(it) }
