package org.readium.r2.navigator.input

import android.graphics.PointF

/**
 * Represents a tap event emitted by a navigator at the given [point].
 *
 * All the points are relative to the navigator view.
 */
public data class TapEvent(
    val point: PointF,
)
