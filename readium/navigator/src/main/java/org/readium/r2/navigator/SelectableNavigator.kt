/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.graphics.RectF
import org.readium.r2.shared.publication.Locator

/**
 * A navigator supporting user selection.
 */
public interface SelectableNavigator : Navigator {

    /** Currently selected content. */
    public suspend fun currentSelection(): Selection?

    /** Clears the current selection. */
    public fun clearSelection()
}

/**
 * Represents a user content selection in a navigator.
 *
 * @param locator Location of the user selection in the publication.
 * @param rect Frame of the bounding rect for the selection, in the coordinate of the navigator
 *        view. This is only useful in the context of a VisualNavigator.
 */
public data class Selection(
    val locator: Locator,
    val rect: RectF?,
)
