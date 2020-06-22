/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

// This file will be merged into `PositionsService` in the next release.

/**
 * Returns the list of all the positions in the publication.
 */
suspend fun Publication.positions(): List<Locator> {
    if (!isPositionInitialized) {
        _positions = withContext(Dispatchers.IO) {
            positionsFactory?.create() ?: emptyList()
        }
    }
    return _positions
}

/**
 * Returns the list of all the positions in the publication, grouped by the resource reading
 * order index.
 */
suspend fun Publication.positionsByReadingOrder(): List<List<Locator>> {
    val locators = positions().groupBy(Locator::href)
    return readingOrder.map { locators[it.href].orEmpty() }
}
