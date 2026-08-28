/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.readium.navigator.web.internals.gestures

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity

/**
 * Interface to specify fling behavior.
 *
 * When drag has ended with velocity in [scrollable2D], [performFling] is invoked to perform fling
 * animation and update state via [Scroll2DScope.scrollBy]
 */
@Stable
public interface Fling2DBehavior {
    /**
     * Perform settling via fling animation with given velocity and suspend until fling has
     * finished.
     *
     * This functions is called with [Scroll2DScope] to drive the state change of the
     * [Scrollable2DState] via [Scroll2DScope.scrollBy].
     *
     * This function must return correct velocity left after it is finished flinging in order to
     * guarantee proper nested scroll support.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     * [scrollable2D] that invoked this method.
     *
     * @return remaining velocity after fling operation has ended
     */
    public suspend fun Scroll2DScope.performFling(initialVelocity: Velocity): Velocity
}

internal class ConsumingFling2DBehavior : Fling2DBehavior {

    override suspend fun Scroll2DScope.performFling(
        initialVelocity: Velocity,
    ): Velocity {
        return Velocity.Zero
    }
}

internal class NullFling2DBehavior : Fling2DBehavior {

    override suspend fun Scroll2DScope.performFling(
        initialVelocity: Velocity,
    ): Velocity {
        return initialVelocity
    }
}

public fun FlingBehavior.toFling2DBehavior(orientation: Orientation): Fling2DBehavior =
    object : Fling2DBehavior {
        override suspend fun Scroll2DScope.performFling(
            initialVelocity: Velocity,
        ): Velocity {
            val scrollScope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float =
                    when (orientation) {
                        Orientation.Vertical -> scrollBy(Offset(0f, pixels)).y
                        Orientation.Horizontal -> scrollBy(Offset(pixels, 0f)).x
                    }
            }

            val velocity =
                when (orientation) {
                    Orientation.Vertical -> initialVelocity.y
                    Orientation.Horizontal -> initialVelocity.x
                }

            val remainingVelocity =
                scrollScope.performFling(velocity)

            return when (orientation) {
                Orientation.Vertical -> Velocity(0f, remainingVelocity)
                Orientation.Horizontal -> Velocity(remainingVelocity, 0f)
            }
        }
    }
