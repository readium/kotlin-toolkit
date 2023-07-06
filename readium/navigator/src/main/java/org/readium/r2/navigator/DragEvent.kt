package org.readium.r2.navigator

import android.graphics.PointF

/**
 * Represents a drag event emitted by a [Navigator] from a [start] point moved by an [offset].
 *
 * All the points are relative to the navigator view.
 */
data class DragEvent(
    val type: Type,
    val start: PointF,
    val offset: PointF
) {
    enum class Type { Start, Move, End }
}