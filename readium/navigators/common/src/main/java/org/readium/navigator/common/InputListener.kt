/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.times
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A listener for input events.
 */
@ExperimentalReadiumApi
public interface InputListener {
    /**
     * Called when the user tapped the content and nothing handled the event (eg.
     * taps on links are not reported here).
     */
    public fun onTap(event: TapEvent, context: TapContext)
}

/**
 * Represents a tap event at the given [offset].
 *
 * All the offsets are relative to the rendition view.
 */
@ExperimentalReadiumApi
public data class TapEvent(
    val offset: DpOffset,
)

/**
 * Provides additional context for the tap event.
 */
@ExperimentalReadiumApi
public data class TapContext(
    val viewport: DpSize,
)

@ExperimentalReadiumApi
private class NullInputListener : InputListener {
    override fun onTap(event: TapEvent, context: TapContext) {
    }
}

/**
 * The default [InputListener], handling directional UI events (e.g. edge taps or arrow keys) to
 * turn the pages of a visual rendition through an [OverflowController].
 *
 * This takes into account the reading progression of the navigator to turn pages in the right
 * direction.
 *
 * @param controller an [OverflowController] to use for navigation
 * @param tapEdges: Indicates which viewport edges handle taps.
 * @param handleTapsWhileScrolling: Indicates whether the page turns should be handled when the
 *        publication is scrollable.
 * @param minimumHorizontalEdgeSize: The minimum horizontal edge dimension triggering page turns, in
 *        pixels.
 * @param horizontalEdgeThresholdPercent: The percentage of the viewport dimension used to compute
 *        the horizontal edge size. When null, minimumHorizontalEdgeSize will be used instead.
 * @param minimumVerticalEdgeSize: The minimum vertical edge dimension triggering page turns, in
 *        pixels.
 * @param verticalEdgeThresholdPercent: The percentage of the viewport dimension used to compute the
 *        vertical edge size. When null, minimumVerticalEdgeSize will be used instead.
 */
@ExperimentalReadiumApi
@Composable
public fun defaultInputListener(
    controller: OverflowController?,
    fallbackListener: InputListener? = null,
    tapEdges: Set<Orientation> = setOf(
        Orientation.Horizontal
    ),
    handleTapsWhileScrolling: Boolean = false,
    minimumHorizontalEdgeSize: Dp = 80.0.dp,
    horizontalEdgeThresholdPercent: Double? = 0.3,
    minimumVerticalEdgeSize: Dp = 80.0.dp,
    verticalEdgeThresholdPercent: Double? = 0.3,
): InputListener {
    val coroutineScope = rememberCoroutineScope()

    return remember(controller) {
        if (controller == null) {
            NullInputListener()
        } else {
            DefaultInputListener(
                coroutineScope,
                fallbackListener,
                controller,
                tapEdges,
                handleTapsWhileScrolling,
                minimumHorizontalEdgeSize,
                horizontalEdgeThresholdPercent,
                minimumVerticalEdgeSize,
                verticalEdgeThresholdPercent
            )
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private class DefaultInputListener(
    private val coroutineScope: CoroutineScope,
    private val fallbackListener: InputListener?,
    private val controller: OverflowController,
    private val tapEdges: Set<Orientation>,
    private val handleTapsWhileScrolling: Boolean,
    private val minimumHorizontalEdgeSize: Dp,
    private val horizontalEdgeThresholdPercent: Double?,
    private val minimumVerticalEdgeSize: Dp,
    private val verticalEdgeThresholdPercent: Double?,
) : InputListener {

    override fun onTap(event: TapEvent, context: TapContext) {
        if (!handleTap(event, context)) {
            fallbackListener?.onTap(event, context)
        }
    }

    private fun handleTap(event: TapEvent, context: TapContext): Boolean {
        if (controller.overflow.scroll && !handleTapsWhileScrolling) {
            return false
        }

        if (tapEdges.contains(Orientation.Horizontal)) {
            val width = context.viewport.width

            val horizontalEdgeSize = horizontalEdgeThresholdPercent
                ?.let { max(minimumHorizontalEdgeSize, it * width) }
                ?: minimumHorizontalEdgeSize
            val leftRange = 0.0.dp..horizontalEdgeSize
            val rightRange = (width - horizontalEdgeSize)..width

            if (event.offset.x in rightRange && controller.canMoveRight) {
                coroutineScope.launch { controller.moveRight() }
                return true
            } else if (event.offset.x in leftRange && controller.canMoveLeft) {
                coroutineScope.launch { controller.moveLeft() }
                return true
            }
        }

        if (tapEdges.contains(Orientation.Vertical)) {
            val height = context.viewport.height

            val verticalEdgeSize = verticalEdgeThresholdPercent
                ?.let { max(minimumVerticalEdgeSize, it * height) }
                ?: minimumVerticalEdgeSize
            val topRange = 0.0.dp..verticalEdgeSize
            val bottomRange = (height - verticalEdgeSize)..height

            if (event.offset.y in bottomRange && controller.canMoveForward) {
                coroutineScope.launch { controller.moveForward() }
                return true
            } else if (event.offset.y in topRange && controller.canMoveBackward) {
                coroutineScope.launch { controller.moveBackward() }
                return true
            }
        }

        return false
    }

    private val OverflowController.canMoveLeft get() =
        when (overflow.readingProgression) {
            ReadingProgression.LTR ->
                canMoveBackward

            ReadingProgression.RTL ->
                canMoveForward
        }

    private val OverflowController.canMoveRight get() =
        when (overflow.readingProgression) {
            ReadingProgression.LTR ->
                canMoveForward

            ReadingProgression.RTL ->
                canMoveBackward
        }
}
