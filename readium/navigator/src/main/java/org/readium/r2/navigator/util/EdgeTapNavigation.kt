package org.readium.r2.navigator.util

import android.graphics.PointF
import android.view.View
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Convenience utility to handle page turns when tapping the edge of the screen.
 *
 * Call [EdgeTapNavigation.onTap] from the [VisualNavigator.Listener.onTap] callback to turn pages
 * automatically.
 *
 * @param navigator Navigator used to turn pages.
 * @param minimumEdgeSize The minimum edge dimension triggering page turns, in pixels.
 * @param edgeThresholdPercent The percentage of the viewport dimension used to compute the edge
 *        dimension. When null, minimumEdgeSize will be used instead.
 * @param animatedTransition Indicates whether the page turns should be animated.
 */
@OptIn(ExperimentalReadiumApi::class)
class EdgeTapNavigation(
    private val navigator: VisualNavigator,
    private val minimumEdgeSize: Double = 200.0,
    private val edgeThresholdPercent: Double? = 0.3,
    private val animatedTransition: Boolean = false,
    private val handleTapsWhileScrolling: Boolean = false
) {
    private enum class Transition {
        FORWARD, BACKWARD, NONE;

        fun reverse() = when (this) {
            FORWARD -> BACKWARD
            BACKWARD -> FORWARD
            NONE -> NONE
        }
    }

    /**
     * Handles a tap in the navigator viewport and returns whether it was successful.
     *
     * To be called from [VisualNavigator.Listener.onTap].
     *
     * @param view Navigator view from which the point is relative.
     */
    fun onTap(point: PointF, view: View): Boolean {
        val horizontalEdgeSize by lazy {
            if (edgeThresholdPercent != null)
                (edgeThresholdPercent * view.width).coerceAtLeast(minimumEdgeSize)
            else minimumEdgeSize
        }
        val leftRange by lazy { 0.0..horizontalEdgeSize }
        val rightRange by lazy { (view.width - horizontalEdgeSize)..view.width.toDouble() }

        val verticalEdgeSize by lazy {
            if (edgeThresholdPercent != null)
                (edgeThresholdPercent * view.height).coerceAtLeast(minimumEdgeSize)
            else minimumEdgeSize
        }
        val topRange by lazy { 0.0..verticalEdgeSize }
        val bottomRange by lazy { (view.height - verticalEdgeSize)..view.height.toDouble() }

        val presentation = navigator.presentation.value

        var transition: Transition =
            when {
                presentation.scroll && !handleTapsWhileScrolling ->
                    Transition.NONE
                presentation.axis == Axis.HORIZONTAL ->
                    when {
                        rightRange.contains(point.x) -> Transition.FORWARD
                        leftRange.contains(point.x) -> Transition.BACKWARD
                        else -> Transition.NONE
                    }
                else -> when {
                    bottomRange.contains(point.y) -> Transition.FORWARD
                    topRange.contains(point.y) -> Transition.BACKWARD
                    else -> Transition.NONE
                }
            }

        if (presentation.readingProgression == ReadingProgression.RTL) {
            transition = transition.reverse()
        }

        return when (transition) {
            Transition.FORWARD -> navigator.goForward(animated = animatedTransition)
            Transition.BACKWARD -> navigator.goBackward(animated = animatedTransition)
            Transition.NONE -> false
        }
    }
}
