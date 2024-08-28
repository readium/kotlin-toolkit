package org.readium.navigator.web.logging

import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import timber.log.Timber

internal class LoggingTargetedFlingBehavior(
    private val delegate: TargetedFlingBehavior
) : TargetedFlingBehavior {

    override suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onRemainingDistanceUpdated: (Float) -> Unit
    ): Float {
        Timber.d("performFling $initialVelocity")
        return with(delegate) {
            performFling(initialVelocity, onRemainingDistanceUpdated)
        }
    }
}
