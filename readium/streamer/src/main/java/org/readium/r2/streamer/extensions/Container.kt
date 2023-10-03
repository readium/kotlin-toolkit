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
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.readAsXml
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.xml.ElementNode

/** Returns the resource data as an XML Document at the given [path], or null. */
internal suspend fun Container.readAsXmlOrNull(path: String): ElementNode? =
    Url.fromDecodedPath(path)?.let { readAsXmlOrNull(it) }

/** Returns the resource data as an XML Document at the given [url], or null. */
internal suspend fun Container.readAsXmlOrNull(url: Url): ElementNode? =
    get(url).use { it.readAsXml().getOrNull() }

internal suspend fun Container.guessTitle(): String? {
    val entries = entries() ?: return null
    val firstEntry = entries.firstOrNull() ?: return null
    val commonFirstComponent = entries.pathCommonFirstComponent() ?: return null

    if (commonFirstComponent.name == firstEntry.url.path) {
        return null
    }

    return commonFirstComponent.name
}

/** Returns a [File] to the directory containing all paths, if there is such a directory. */
internal fun Iterable<Container.Entry>.pathCommonFirstComponent(): File? =
    mapNotNull { it.url.path?.substringBefore("/") }
        .distinct()
        .takeIf { it.size == 1 }
        ?.firstOrNull()
        ?.let { File(it) }
