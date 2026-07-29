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
 * The navigator used to report page positions one too high: a stored
 * `locations.position` is `actual page + 1`, and `locations.totalProgression` is shifted
 * accordingly. Re-resolving the corrected page from `positions()` fixes both.
 *
 * Only use this API when you are upgrading and migrating the locators (bookmarks,
 * reading progression) stored in your database, and only for locators created by the
 * Pdfium navigator — locators your app computed itself from `positions()` are
 * unaffected. Apply it once per stored locator. See the migration guide for more
 * information.
 */
@DelicateReadiumApi
public suspend fun Publication.migrateLegacyPdfiumLocator(locator: Locator): Locator {
    val position = locator.locations.position ?: return locator
    // Two different -1s are stacked here, and only the first is the bug fix:
    //   -1 to undo the off-by-one in the stored position;
    //   -1 more to convert the (by-design) 1-based `position` into an index into the
    //      0-based `positions()` list, whose element k carries `position == k + 1`.
    // (`getOrNull(position - 1)` would be a no-op: it returns the element whose
    // `position` equals the stored value.)
    return positions().getOrNull(position - 2)
        ?: locator // Position 1 (only ever the initial value, i.e. page 1) or out of
    // range: correct as-is, keep unchanged.
}
