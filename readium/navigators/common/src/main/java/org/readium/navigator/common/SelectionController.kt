/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.ui.unit.DpRect
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
/**
 * A controller for selection.
 */
public interface SelectionController<S : SelectionLocation> {

    /** Gets the currently selected content at the call time or a bit later. */
    public suspend fun currentSelection(): Selection<S>?

    /** Clears the current selection. */
    public fun clearSelection()
}

/**
 * Represents a user content selection in a navigator.
 *
 * @param text The user selected text.
 * @param rect Frame of the bounding rect for the selection, in the coordinate of the navigator
 *        view. This is only useful in the context of a VisualNavigator.
 */
@ExperimentalReadiumApi
public data class Selection<S : SelectionLocation>(
    val text: String,
    val rect: DpRect,
    val location: S,
)

/**
 * Marker interface for a [Location] locating a selection.
 */
@ExperimentalReadiumApi
public interface SelectionLocation : ExportableLocation
