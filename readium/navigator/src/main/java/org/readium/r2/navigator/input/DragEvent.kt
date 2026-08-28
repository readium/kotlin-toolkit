package org.readium.r2.navigator.input

import android.graphics.PointF

/**
 * Represents a drag event emitted by a navigator from a [start] point moved by an [offset].
 *
 * All the points are relative to the navigator view.
 */
public data class DragEvent(
    val type: Type,
    val start: PointF,
    val offset: PointF,
) {
    public enum class Type { Start, Move, End }
}
