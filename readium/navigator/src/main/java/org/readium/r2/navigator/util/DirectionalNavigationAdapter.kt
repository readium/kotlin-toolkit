package org.readium.r2.navigator.util

import org.readium.r2.navigator.DirectionalNavigator
import org.readium.r2.navigator.goLeft
import org.readium.r2.navigator.goRight
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Helper handling directional UI events (e.g. edge taps or arrow keys) to turn the pages of a
 * VisualNavigator.
 *
 * This takes into account the reading progression of the navigator to turn pages in the right
 * direction.
 *
 * Add it to a navigator with `addInputListener(DirectionalNavigationAdapter())`.
 *
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
 * @param animatedTransition: Indicates whether the page turns should be animated.
 */
@ExperimentalReadiumApi
public class DirectionalNavigationAdapter(
    private val navigator: DirectionalNavigator,
    private val tapEdges: Set<TapEdge> = setOf(TapEdge.Horizontal),
    private val handleTapsWhileScrolling: Boolean = false,
    private val minimumHorizontalEdgeSize: Double = 80.0,
    private val horizontalEdgeThresholdPercent: Double? = 0.3,
    private val minimumVerticalEdgeSize: Double = 80.0,
    private val verticalEdgeThresholdPercent: Double? = 0.3,
    private val animatedTransition: Boolean = false
) : InputListener {

    /**
     * Indicates which viewport edges trigger page turns on tap.
     */
    public enum class TapEdge {
        Horizontal, Vertical;
    }

    override fun onTap(event: TapEvent): Boolean {
        if (navigator.presentation.value.scroll && !handleTapsWhileScrolling) {
            return false
        }

        if (tapEdges.contains(TapEdge.Horizontal)) {
            val width = navigator.publicationView.width.toDouble()

            val horizontalEdgeSize = horizontalEdgeThresholdPercent?.let {
                maxOf(minimumHorizontalEdgeSize, it * width)
            } ?: minimumHorizontalEdgeSize
            val leftRange = 0.0..horizontalEdgeSize
            val rightRange = (width - horizontalEdgeSize)..width

            if (event.point.x in rightRange) {
                return navigator.goRight(animated = animatedTransition)
            } else if (event.point.x in leftRange) {
                return navigator.goLeft(animated = animatedTransition)
            }
        }

        if (tapEdges.contains(TapEdge.Vertical)) {
            val height = navigator.publicationView.height.toDouble()

            val verticalEdgeSize = verticalEdgeThresholdPercent?.let {
                maxOf(minimumVerticalEdgeSize, it * height)
            } ?: minimumVerticalEdgeSize
            val topRange = 0.0..verticalEdgeSize
            val bottomRange = (height - verticalEdgeSize)..height

            if (event.point.y in bottomRange) {
                return navigator.goForward(animated = animatedTransition)
            } else if (event.point.y in topRange) {
                return navigator.goBackward(animated = animatedTransition)
            }
        }

        return false
    }

    override fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEvent.Type.Down || event.modifiers.isNotEmpty()) {
            return false
        }

        return when (event.key) {
            Key.ArrowUp -> navigator.goBackward(animated = animatedTransition)
            Key.ArrowDown, Key.Space -> navigator.goForward(animated = animatedTransition)
            Key.ArrowLeft -> navigator.goLeft(animated = animatedTransition)
            Key.ArrowRight -> navigator.goRight(animated = animatedTransition)
            else -> false
        }
    }
}
