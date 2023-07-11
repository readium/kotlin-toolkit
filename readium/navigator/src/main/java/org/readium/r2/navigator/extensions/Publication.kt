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
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions

// These extensions will be removed in the next release, with `PositionsService`.

internal val Publication.positionsSync: List<Locator>
    get() = runBlocking { positions() }

internal val Publication.positionsByResource: Map<String, List<Locator>>
    get() = runBlocking { positions().groupBy { it.href } }
