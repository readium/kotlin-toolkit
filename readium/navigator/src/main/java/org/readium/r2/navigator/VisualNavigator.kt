/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.graphics.PointF
import android.view.View
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.ReadingProgression

/**
 * A navigator rendering the publication visually on-screen.
 */
public interface VisualNavigator : Navigator {

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

    public interface Listener : Navigator.Listener {

        @Deprecated("Use `addInputListener` instead", level = DeprecationLevel.ERROR)
        public fun onTap(point: PointF): Boolean = false

        @Deprecated("Use `addInputListener` instead", level = DeprecationLevel.ERROR)
        public fun onDragStart(startPoint: PointF, offset: PointF): Boolean = false

        @Deprecated("Use `addInputListener` instead", level = DeprecationLevel.ERROR)
        public fun onDragMove(startPoint: PointF, offset: PointF): Boolean = false

        @Deprecated("Use `addInputListener` instead", level = DeprecationLevel.ERROR)
        public fun onDragEnd(startPoint: PointF, offset: PointF): Boolean = false
    }

    /**
     * Current reading progression direction.
     */
    @Deprecated(
        "Use `presentation.value.readingProgression` instead",
        ReplaceWith("presentation.value.readingProgression"),
        level = DeprecationLevel.ERROR
    )
    public val readingProgression: ReadingProgression
}

/**
 * A navigator able to move backward and forward in a publication.
 */
@ExperimentalReadiumApi
public interface DirectionalNavigator : VisualNavigator {

    public interface Listener : VisualNavigator.Listener

    /**
     * Current presentation rendered by the navigator.
     */
    @ExperimentalReadiumApi
    public val presentation: StateFlow<Presentation>

    @ExperimentalReadiumApi
    public interface Presentation {
        /**
         * Horizontal direction of progression across resources.
         */
        public val readingProgression: org.readium.r2.navigator.preferences.ReadingProgression

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
    public fun goForward(animated: Boolean = false, completion: () -> Unit = {}): Boolean

    /**
     * Moves to the previous content portion (eg. page) in the reading progression direction.
     */
    public fun goBackward(animated: Boolean = false, completion: () -> Unit = {}): Boolean
}

/**
 * Moves to the left content portion (eg. page) relative to the reading progression direction.
 */
@ExperimentalReadiumApi
public fun DirectionalNavigator.goLeft(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    return when (presentation.value.readingProgression) {
        org.readium.r2.navigator.preferences.ReadingProgression.LTR ->
            goBackward(animated = animated, completion = completion)

        org.readium.r2.navigator.preferences.ReadingProgression.RTL ->
            goForward(animated = animated, completion = completion)
    }
}

/**
 * Moves to the right content portion (eg. page) relative to the reading progression direction.
 */
@ExperimentalReadiumApi
public fun DirectionalNavigator.goRight(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    return when (presentation.value.readingProgression) {
        org.readium.r2.navigator.preferences.ReadingProgression.LTR ->
            goForward(animated = animated, completion = completion)

        org.readium.r2.navigator.preferences.ReadingProgression.RTL ->
            goBackward(animated = animated, completion = completion)
    }
}
