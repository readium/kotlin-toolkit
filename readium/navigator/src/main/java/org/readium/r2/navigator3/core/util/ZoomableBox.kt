package org.readium.r2.navigator3.core.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import org.readium.r2.navigator3.core.gestures.*

@Composable
internal fun ZoomableBox(
    modifier: Modifier = Modifier,
    state: ZoomableBoxState = rememberZoomableBoxState(mutableStateOf(1f)),
    reverseScrollDirection: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .logConstraints("zoomableBoxInput")
            .scrollable(
                horizontalState = state.horizontalScrollState,
                verticalState = state.verticalScrollState,
                reverseDirection = reverseScrollDirection,
            )
            .scrolling(
                state = state.verticalScrollState,
                isVertical = true,
                reverseScrolling = reverseScrollDirection,
            )
            .scrolling(
                state = state.horizontalScrollState,
                isVertical = false,
                reverseScrolling = reverseScrollDirection
            )
            .logConstraints("zoomableBoxAfter")
            .zoomable(state),
        Alignment.Center
    ) {
        content()
    }
}

@Composable
internal fun rememberZoomableBoxState(
    scaleState: MutableState<Float> = mutableStateOf(1f),
    horizontalScrollState: ScrollState = ScrollState(0),
    verticalScrollState: ScrollState = ScrollState(0)
) = remember {
    ZoomableBoxState(scaleState, horizontalScrollState, verticalScrollState)
}

internal class ZoomableBoxState(
    override val scaleState: MutableState<Float>,
    val horizontalScrollState: ScrollState,
    val verticalScrollState: ScrollState
) : ZoomableState {
    override fun onScaleChanged(zoomChange: Float, centroid: Offset) {
        val horizontalDelta = centroid.x * zoomChange - centroid.x
        horizontalScrollState.dispatchRawDelta(horizontalDelta)

        val verticalDelta = centroid.y * zoomChange - centroid.y
        verticalScrollState.dispatchRawDelta(verticalDelta)
    }
}
