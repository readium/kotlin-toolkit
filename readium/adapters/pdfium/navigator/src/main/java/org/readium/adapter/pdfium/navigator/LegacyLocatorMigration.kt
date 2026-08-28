/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pdfium.navigator

import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions

/**
 * Corrects a PDF [locator] persisted by the Pdfium navigator before the page-index fix,
 * by re-resolving it from this publication's position list.
 *
 * The Pdfium navigator used to report page positions one too high: a stored
 * `locations.position` is `actual page + 1`, and the page fragment and progressions are
 * shifted accordingly.
 *
 * Only use this API when you are upgrading and migrating the locators (bookmarks,
 * reading progression) stored in your database, and only for locators created by the
 * Pdfium navigator — locators your app computed itself from `positions()` are
 * unaffected. Apply it once per stored locator. See the migration guide for more
 * information.
 */
@DelicateReadiumApi
public suspend fun Publication.migrateLegacyPdfiumLocator(locator: Locator): Locator {
    val storedPosition = locator.locations.position ?: return locator
    // Undo the off-by-one to get the position of the page actually being read.
    val correctedPosition = storedPosition - 1
    // `position` is 1-based by design, while `positions()` is a 0-based list whose
    // element k carries `position == k + 1`.
    val corrected = positions().getOrNull(correctedPosition - 1)
        // Position 1 (only ever the initial value, i.e. page 1) or out of range: keep
        // the locator unchanged.
        ?: return locator

    // The whole locations are replaced, as the page fragment and the progressions are
    // shifted as well and must stay consistent with the position.
    return locator.copy(
        locations = corrected.locations.copy(
            otherLocations = locator.locations.otherLocations + corrected.locations.otherLocations
        )
    )
}
