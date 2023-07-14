/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import org.json.JSONObject
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.util.use

/** Returns the resource data as an XML Document at the given [href], or null. */
internal suspend fun Fetcher.readAsXmlOrNull(href: String): ElementNode? =
    get(href).use { it.readAsXml().getOrNull() }

/** Returns the resource data as a JSON object at the given [href], or null. */
internal suspend fun Fetcher.readAsJsonOrNull(href: String): JSONObject? =
    get(href).use { it.readAsJson().getOrNull() }

internal suspend fun Fetcher.guessTitle(): String? {
    val firstLink = links().firstOrNull() ?: return null
    val commonFirstComponent = links().hrefCommonFirstComponent() ?: return null

    if (commonFirstComponent.name == firstLink.href.removePrefix("/"))
        return null

    return commonFirstComponent.name
}
