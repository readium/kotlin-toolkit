/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi

/**
 * This controller enables navigation through the viewport of an overflowing publication.
 */
@ExperimentalReadiumApi
public interface OverflowController {

    /**
     * Information about the current presentation of the rendition.
     */
    public val overflow: Overflow

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

/**
 * Holds information about the presentation of a publication.
 */
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

@InternalReadiumApi
@OptIn(ExperimentalReadiumApi::class)
public data class SimpleOverflow(
    override val readingProgression: ReadingProgression,
    override val scroll: Boolean,
    override val axis: Axis,
) : Overflow

/**
 * Moves to the left content portion (eg. page) relative to the reading progression direction.
 */
@ExperimentalReadiumApi
public suspend fun OverflowController.moveLeft() {
    return when (overflow.readingProgression) {
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
    return when (overflow.readingProgression) {
        ReadingProgression.LTR ->
            moveForward()

        ReadingProgression.RTL ->
            moveBackward()
    }
}
