package org.readium.navigator.web.gestures

import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Velocity

/**
 * Interface to specify fling behavior.
 *
 * When drag has ended with velocity in [scrollable], [performFling] is invoked to perform fling
 * animation and update state via [ScrollScope.scrollBy]
 */
@Stable
internal interface Fling2DBehavior {
    /**
     * Perform settling via fling animation with given velocity and suspend until fling has
     * finished.
     *
     * This functions is called with [ScrollScope] to drive the state change of the
     * [androidx.compose.foundation.gestures.ScrollableState] via [ScrollScope.scrollBy].
     *
     * This function must return correct velocity left after it is finished flinging in order to
     * guarantee proper nested scroll support.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     * [androidx.compose.foundation.gestures.scrollable] that invoked this method.
     *
     * @return remaining velocity after fling operation has ended
     */
    suspend fun Scroll2DScope.performFling(initialVelocity: Velocity): Velocity
}
