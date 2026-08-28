/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.view.View
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

/**
 * A navigator rendering the publication visually on-screen.
 */
public interface VisualNavigator : Navigator {

    public interface Presentation {
        /**
         * Horizontal direction of progression across resources.
         */
        public val readingProgression: ReadingProgression

        /**
         * If the overflow of the content is managed through scroll instead of pagination.
         */
        public val scroll: Boolean

        /**
         * Main axis along which the resources are laid out.
         */
        public val axis: Axis
    }

    /**
     * View displaying the publication.
     */
    public val publicationView: View

    /**
     * Returns the [Locator] to the first content element that begins on the current screen.
     */
    @ExperimentalReadiumApi
    public suspend fun firstVisibleElementLocator(): Locator? =
        currentLocator.value

    /**
     * Adds a new [InputListener] to receive touch, mouse or keyboard events.
     *
     * Registration order is critical, as listeners may consume the events and prevent others from
     * receiving them.
     */
    @ExperimentalReadiumApi
    public fun addInputListener(listener: InputListener)

    /**
     * Removes a previously registered [InputListener].
     */
    @ExperimentalReadiumApi
    public fun removeInputListener(listener: InputListener)

    public interface Listener : Navigator.Listener
}

/**
 * A [VisualNavigator] with content that can extend beyond the viewport.
 *
 * The user typically navigates through the publication by scrolling or tapping the viewport edges.
 */
@ExperimentalReadiumApi
public interface OverflowableNavigator : VisualNavigator {

    @ExperimentalReadiumApi
    public interface Listener : VisualNavigator.Listener

    /**
     * Current presentation rendered by the navigator.
     */
    @ExperimentalReadiumApi
    public val overflow: StateFlow<Overflow>

    @ExperimentalReadiumApi
    public interface Overflow {
        /**
         * Horizontal direction of progression across resources.
         */
        public val readingProgression: ReadingProgression

        /**
         * If the overflow of the content is managed through scroll instead of pagination.
         */
        public val scroll: Boolean

        /**
         * Main axis along which the resources are laid out.
         */
        public val axis: Axis
    }

    /**
     * Moves to the next content portion (eg. page) in the reading progression direction.
     */
    public fun goForward(animated: Boolean = false): Boolean

    /**
     * Moves to the previous content portion (eg. page) in the reading progression direction.
     */
    public fun goBackward(animated: Boolean = false): Boolean
}
