/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.extensions

import java.net.URL
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions

/** Computes an absolute URL to the given HREF. */
internal fun Publication.urlToHref(href: String): URL? {
    val baseUrl = this.baseUrl?.toString()?.removeSuffix("/")
    val urlString = if (baseUrl != null && href.startsWith("/")) {
        "$baseUrl$href"
    } else {
        href
    }
    return tryOrNull { URL(urlString) }
}

// These extensions will be removed in the next release, with `PositionsService`.

internal val Publication.positionsSync: List<Locator>
    get() = runBlocking { positions() }

internal val Publication.positionsByResource: Map<String, List<Locator>>
    get() = runBlocking { positions().groupBy { it.href } }
