/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import timber.log.Timber

internal class LoggingTargetedFlingBehavior(
    private val delegate: TargetedFlingBehavior,
) : TargetedFlingBehavior {

    override suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onRemainingDistanceUpdated: (Float) -> Unit,
    ): Float {
        Timber.d("performFling $initialVelocity")
        return with(delegate) {
            performFling(initialVelocity, onRemainingDistanceUpdated)
        }
    }
}
