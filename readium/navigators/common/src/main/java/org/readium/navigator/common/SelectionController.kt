/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.runtime.State
import androidx.compose.ui.unit.DpRect
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A navigator supporting user selection.
 */
@ExperimentalReadiumApi
public interface Selectable<S : SelectionLocation> {

    /** Currently selected content. */
    public suspend fun currentSelection(): State<Selection<S>?>

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
    val position: S,
)

@ExperimentalReadiumApi
public interface SelectionLocation
