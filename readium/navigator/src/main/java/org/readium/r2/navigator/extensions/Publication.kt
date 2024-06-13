/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.extensions

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

// These extensions will be removed in the next release, with `PositionsService`.

internal val Publication.positionsByResource: Map<Url, List<Locator>>
    get() = runBlocking { positions().groupBy { it.href } }

/**
 * Historically, we used to have "absolute" HREFs in the manifest:
 *   - starting with a `/` for packaged publications.
 *   - resolved to the `self` link for remote publications.
 *
 * We removed the normalization and now use relative HREFs everywhere, but we still need to support
 * the locators created with the old absolute HREFs.
 */
@DelicateReadiumApi
public fun Publication.normalizeLocator(locator: Locator): Locator {
    val self = (baseUrl as? AbsoluteUrl)

    return if (self == null) { // Packaged publication
        locator.copy(
            href = Url(locator.href.toString().removePrefix("/"))
                ?: return locator
        )
    } else { // Remote publication
        // Check that the locator HREF relative to `self` exists in the manifest.
        val relativeHref = self.relativize(locator.href)
        if (linkWithHref(relativeHref) != null) {
            locator.copy(href = relativeHref)
        } else {
            locator
        }
    }
}
