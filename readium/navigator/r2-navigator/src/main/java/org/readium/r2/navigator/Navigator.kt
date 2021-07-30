/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.graphics.PointF
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.ReadingProgression

@OptIn(ExperimentalCoroutinesApi::class)
interface Navigator {

    /**
     * Current position in the publication.
     * Can be used to save a bookmark to the current position.
     */
    val currentLocator: StateFlow<Locator>

    /**
     * Moves to the position in the publication corresponding to the given [Locator].
     */
    fun go(locator: Locator, animated: Boolean = false, completion: () -> Unit = {}): Boolean

    /**
     * Moves to the position in the publication targeted by the given link.
     */
    fun go(link: Link, animated: Boolean = false, completion: () -> Unit = {}): Boolean

    /**
     * Moves to the next content portion (eg. page) in the reading progression direction.
     */
    fun goForward(animated: Boolean = false, completion: () -> Unit = {}): Boolean

    /**
     * Moves to the previous content portion (eg. page) in the reading progression direction.
     */
    fun goBackward(animated: Boolean = false, completion: () -> Unit = {}): Boolean

    interface Listener


    @Deprecated("Use [currentLocator.value] instead", ReplaceWith("currentLocator.value"))
    val currentLocation: Locator? get() = currentLocator.value
    @Deprecated("Use [VisualNavigator.Listener] instead", ReplaceWith("VisualNavigator.Listener"))
    interface VisualListener : VisualNavigator.Listener

}

interface NavigatorDelegate {
    @Deprecated("Observe [currentLocator] instead")
    fun locationDidChange(navigator: Navigator? = null, locator: Locator) {}
}


/**
 * A navigator rendering the publication visually on-screen.
 */
interface VisualNavigator : Navigator {
    /**
     * Current reading progression direction.
     */
    val readingProgression: ReadingProgression

    interface Listener : Navigator.Listener {
        /**
         * Called when the user tapped the publication, and it didn't trigger any internal action.
         * The point is relative to the navigator's view.
         */
        fun onTap(point: PointF): Boolean = false
    }
}

/**
 * Moves to the left content portion (eg. page) relative to the reading progression direction.
 */
fun VisualNavigator.goLeft(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    return when (readingProgression) {
        ReadingProgression.LTR, ReadingProgression.TTB, ReadingProgression.AUTO ->
            goBackward(animated = animated, completion = completion)

        ReadingProgression.RTL, ReadingProgression.BTT ->
            goForward(animated = animated, completion = completion)
    }
}

/**
 * Moves to the right content portion (eg. page) relative to the reading progression direction.
 */
fun VisualNavigator.goRight(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    return when (readingProgression) {
        ReadingProgression.LTR, ReadingProgression.TTB, ReadingProgression.AUTO ->
            goForward(animated = animated, completion = completion)

        ReadingProgression.RTL, ReadingProgression.BTT ->
            goBackward(animated = animated, completion = completion)
    }
}
