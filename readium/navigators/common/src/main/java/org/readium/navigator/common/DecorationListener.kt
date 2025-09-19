/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
/**
 * A listener for events related to decorations.
 */
public interface DecorationListener<in L : DecorationLocation> {

    /**
     * Called when the user activates a decoration, e.g. with a click or tap.
     *
     * @param event Holds the metadata about the interaction event.
     */
    public fun onDecorationActivated(event: OnActivatedEvent<L>)

    /**
     * Holds the metadata about a decoration activation interaction.
     *
     * @param decoration Activated decoration.
     * @param group Name of the group the decoration belongs to.
     * @param rect Frame of the bounding rect for the decoration, in the coordinate of the
     *        rendition view. This is only useful in the context of a visual rendition.
     * @param offset Event point of the interaction, in the coordinate of the navigator view. This is
     *        only useful in the context of a visual rendition.
     */
    public data class OnActivatedEvent<out L : DecorationLocation>(
        val decoration: Decoration<L>,
        val group: String,
        val rect: DpRect? = null,
        val offset: DpOffset? = null,
    )
}

/**
 * The default implementation of [DecorationListener] which does nothing at the moment.
 */
@ExperimentalReadiumApi
public fun <L : DecorationLocation> defaultDecorationListener(
    controller: DecorationController<L>?,
): DecorationListener<L> = NullDecorationListener()

/**
 * A [DecorationListener] which does nothing.
 */
@ExperimentalReadiumApi
internal class NullDecorationListener<L : DecorationLocation> : DecorationListener<L> {

    override fun onDecorationActivated(event: DecorationListener.OnActivatedEvent<L>) {
    }
}
