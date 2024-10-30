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
