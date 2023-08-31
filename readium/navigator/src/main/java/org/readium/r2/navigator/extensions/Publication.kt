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
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions

// These extensions will be removed in the next release, with `PositionsService`.

internal val Publication.positionsSync: List<Locator>
    get() = runBlocking { positions() }

internal val Publication.positionsByResource: Map<String, List<Locator>>
    get() = runBlocking { positions().groupBy { it.href } }

/**
 * Historically, we used to have "absolute" HREFs starting with a `/` in the manifest for packaged
 * publications. We removed the root prefix, but we still need to support the locators created with
 * the old HREFs.
 */
@InternalReadiumApi
public fun Publication.normalizeLocator(locator: Locator): Locator {
    if (linkWithRel("self") != null) {
        return locator
    }

    return locator.copy(href = locator.href.removePrefix("/"))
}
