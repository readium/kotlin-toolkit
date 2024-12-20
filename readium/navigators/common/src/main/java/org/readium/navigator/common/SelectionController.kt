/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.runtime.State
import androidx.compose.ui.unit.DpRect

/**
 * A navigator supporting user selection.
 */
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
public data class Selection<S : SelectionLocation>(
    val text: String,
    val rect: DpRect,
    val position: S,
)

public interface SelectionLocation
