package org.readium.r2.navigator.util

import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.goLeft
import org.readium.r2.navigator.goRight
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.InputModifiers
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
class DirectionalNavigationAdapter(
    private val tapEdges: TapEdges = TapEdges.Horizontal,
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
    @JvmInline
    value class TapEdges(val value: Int) {
        companion object {
            /** The user cannot turn pages by tapping on the edges. */
            val None = TapEdges(0)

            /** The user can turn pages when tapping on the left and right edges. */
            val Horizontal = TapEdges(1 shl 0)

            /** The user can turn pages when tapping on the top and bottom edges. */
            val Vertical = TapEdges(1 shl 1)

            /**
             * The user can turn pages when tapping on the edges of both the horizontal and vertical
             * axes.
             */
            val All = TapEdges(Horizontal.value or Vertical.value)
        }

        fun contains(other: TapEdges): Boolean =
            (value and other.value) == other.value

        operator fun plus(other: TapEdges): TapEdges =
            TapEdges(value or other.value)
    }

    override fun onTap(navigator: VisualNavigator, event: TapEvent): Boolean {
        if (navigator.presentation.value.scroll && !handleTapsWhileScrolling) {
            return false
        }

        if (tapEdges.contains(TapEdges.Horizontal)) {
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

        if (tapEdges.contains(TapEdges.Vertical)) {
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

    override fun onKey(navigator: VisualNavigator, event: KeyEvent): Boolean {
        if (event.type != KeyEvent.Type.Down || event.modifiers != InputModifiers.None) {
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
