package org.readium.navigator.web.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sign

/**
 * Configures the calculations to get the change amount depending on the dragging type.
 * [calculatePostSlopOffset] will return the post offset slop when the touchSlop is reached.
 */
internal interface PointerDirectionConfig {
    fun calculateDeltaChange(offset: Offset): Float
    fun calculatePostSlopOffset(
        totalPositionChange: Offset,
        touchSlop: Float
    ): Offset
}

/**
 * Used for monitoring changes on X axis.
 */
internal val HorizontalPointerDirectionConfig = object : PointerDirectionConfig {
    override fun calculateDeltaChange(offset: Offset): Float = abs(offset.x)

    override fun calculatePostSlopOffset(
        totalPositionChange: Offset,
        touchSlop: Float
    ): Offset {
        val finalMainPositionChange = totalPositionChange.x -
            (sign(totalPositionChange.x) * touchSlop)
        return Offset(finalMainPositionChange, totalPositionChange.y)
    }
}

/**
 * Used for monitoring changes on Y axis.
 */
internal val VerticalPointerDirectionConfig = object : PointerDirectionConfig {
    override fun calculateDeltaChange(offset: Offset): Float = abs(offset.y)

    override fun calculatePostSlopOffset(
        totalPositionChange: Offset,
        touchSlop: Float
    ): Offset {
        val finalMainPositionChange = totalPositionChange.y -
            (sign(totalPositionChange.y) * touchSlop)
        return Offset(totalPositionChange.x, finalMainPositionChange)
    }
}
