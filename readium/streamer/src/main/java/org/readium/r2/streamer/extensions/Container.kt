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
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.decodeXml
import org.readium.r2.shared.util.data.flatDecode
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.xml.ElementNode

/** Returns the resource data as an XML Document at the given [path], or null. */
internal suspend fun Container<*>.readAsXmlOrNull(path: String): ElementNode? =
    Url.fromDecodedPath(path)?.let { readAsXmlOrNull(it) }

/** Returns the resource data as an XML Document at the given [url], or null. */
internal suspend fun Container<*>.readAsXmlOrNull(url: Url): ElementNode? =
    get(url)?.use { resource -> resource.flatDecode { it.decodeXml() }.getOrNull() }

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
