package org.readium.navigator.internal.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

class ConsumeFlingNestedScrollConnection(
    private val consumeHorizontal: Boolean,
    private val consumeVertical: Boolean,
) : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = when (source) {
        // We can consume all resting fling scrolls so that they don't propagate up to the
        // Pager
        NestedScrollSource.Fling -> available.consume(consumeHorizontal, consumeVertical)
        else -> Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        // We can consume all post fling velocity on the main-axis
        // so that it doesn't propagate up to the Pager
        return available.consume(consumeHorizontal, consumeVertical)
    }

    private fun Offset.consume(
        consumeHorizontal: Boolean,
        consumeVertical: Boolean,
    ): Offset = Offset(
        x = if (consumeHorizontal) this.x else 0f,
        y = if (consumeVertical) this.y else 0f,
    )

    private fun Velocity.consume(
        consumeHorizontal: Boolean,
        consumeVertical: Boolean,
    ): Velocity = Velocity(
        x = if (consumeHorizontal) this.x else 0f,
        y = if (consumeVertical) this.y else 0f,
    )
}
