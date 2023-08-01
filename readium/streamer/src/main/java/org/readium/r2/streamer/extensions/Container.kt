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
import org.json.JSONObject
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.readAsJson
import org.readium.r2.shared.resource.readAsXml
import org.readium.r2.shared.util.use

/** Returns the resource data as an XML Document at the given [path], or null. */
internal suspend fun Container.readAsXmlOrNull(path: String): ElementNode? =
    get(path).use { it.readAsXml().getOrNull() }

/** Returns the resource data as a JSON object at the given [path], or null. */
internal suspend fun Container.readAsJsonOrNull(path: String): JSONObject? =
    get(path).use { it.readAsJson().getOrNull() }

internal suspend fun Container.guessTitle(): String? {
    val entries = entries()
    val firstEntry = entries.firstOrNull() ?: return null
    val commonFirstComponent = entries.pathCommonFirstComponent() ?: return null

    if (commonFirstComponent.name == firstEntry.path.removePrefix("/"))
        return null

    return commonFirstComponent.name
}

/** Returns a [File] to the directory containing all paths, if there is such a directory. */
private fun Iterable<Container.Entry>.pathCommonFirstComponent(): File? =
    map { it.path.removePrefix("/").substringBefore("/") }
        .distinct()
        .takeIf { it.size == 1 }
        ?.firstOrNull()
        ?.let { File(it) }
