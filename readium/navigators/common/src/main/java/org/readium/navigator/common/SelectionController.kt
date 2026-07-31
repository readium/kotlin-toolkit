/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.ui.unit.DpRect
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.CopyError
import org.readium.r2.shared.util.Try

/**
 * A controller for selection.
 */
@ExperimentalReadiumApi
public interface SelectionController<S : SelectionLocation> {

    /** Gets the currently selected content at the call time or a bit later. */
    public suspend fun currentSelection(): Selection<S>?

    /** Clears the current selection. */
    public fun clearSelection()

    /**
     * Copies the current selection to the clipboard, after consuming the publication's copy
     * allowance if it is protected.
     *
     * Use this API to implement the Copy item of a custom selection ActionMode callback, instead
     * of writing to the clipboard directly. Otherwise, the copy allowance of a protected
     * publication would be bypassed.
     *
     * @return [CopyError.NoSelection] if there is no selection, [CopyError.Forbidden] if the
     * Content Protection denied the copy. In that case, the clipboard is left untouched.
     */
    public suspend fun copySelection(): Try<Unit, CopyError>
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
