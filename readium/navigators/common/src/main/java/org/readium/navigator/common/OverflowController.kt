/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.runtime.State
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A view with content that can extend beyond the viewport.
 *
 * The user typically navigates through the viewport by scrolling or tapping its edges.
 */
@ExperimentalReadiumApi
public interface OverflowController {

    /**
     * Current presentation rendered by the navigator.
     */
    public val overflow: State<Overflow>

    /**
     * Whether one can move forward through the content or not because the content shown is
     * the end.
     */
    public val canMoveForward: Boolean

    /**
     * Whether one can move backward through the content or not because the content shown is
     * the beginning.
     */
    public val canMoveBackward: Boolean

    /**
     * Moves to the next content portion (eg. page) in the reading progression direction.
     *
     * Does nothing if the end of the content has already been reached.
     */
    public suspend fun moveForward()

    /**
     * Moves to the previous content portion (eg. page) in the reading progression direction.
     *
     * Does nothing if the beginning of the content has already been reached.
     */
    public suspend fun moveBackward()
}

@ExperimentalReadiumApi
public typealias Overflow = org.readium.r2.navigator.OverflowableNavigator.Overflow

/**
 * Moves to the left content portion (eg. page) relative to the reading progression direction.
 */
@ExperimentalReadiumApi
public suspend fun OverflowController.moveLeft() {
    return when (overflow.value.readingProgression) {
        ReadingProgression.LTR ->
            moveBackward()

        ReadingProgression.RTL ->
            moveForward()
    }
}

/**
 * Moves to the right content portion (eg. page) relative to the reading progression direction.
 */
@ExperimentalReadiumApi
public suspend fun OverflowController.moveRight() {
    return when (overflow.value.readingProgression) {
        ReadingProgression.LTR ->
            moveForward()

        ReadingProgression.RTL ->
            moveBackward()
    }
}
