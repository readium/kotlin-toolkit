/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.ExperimentalReadiumApi

public interface DecorationListener {

    /**
     * Called when the user activates a decoration, e.g. with a click or tap.
     *
     * @param event Holds the metadata about the interaction event.
     */
    public fun onDecorationActivated(event: OnActivatedEvent)

    /**
     * Holds the metadata about a decoration activation interaction.
     *
     * @param decoration Activated decoration.
     * @param group Name of the group the decoration belongs to.
     * @param rect Frame of the bounding rect for the decoration, in the coordinate of the
     *        navigator view. This is only useful in the context of a VisualNavigator.
     * @param offset Event point of the interaction, in the coordinate of the navigator view. This is
     *        only useful in the context of a VisualNavigator.
     */
    public data class OnActivatedEvent(
        val decoration: Decoration,
        val group: String,
        val rect: DpRect? = null,
        val offset: DpOffset? = null,
    )
}

@ExperimentalReadiumApi
public fun defaultDecorationListener(
    controller: DecorationController?,
): DecorationListener = NullDecorationListener()

@ExperimentalReadiumApi
internal class NullDecorationListener : DecorationListener {

    override fun onDecorationActivated(event: DecorationListener.OnActivatedEvent) {
    }
}
