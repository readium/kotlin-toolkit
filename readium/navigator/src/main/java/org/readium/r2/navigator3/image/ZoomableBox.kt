package org.readium.r2.navigator3.image

import android.util.Size
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import org.readium.r2.navigator3.core.gestures.*
import timber.log.Timber

@Composable
internal fun ZoomableBox(
    modifier: Modifier = Modifier,
    state: ZoomableBoxState = rememberZoomableBoxState(mutableStateOf(1f)),
    reverseScrollDirection: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier
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
            .zoomable(state)
            .layout { measurable, constraints ->
                Timber.d("layoutConstraints ${constraints.minWidth} ${constraints.minHeight} ${constraints.maxWidth} ${constraints.maxHeight}")
                /*val intrinsicWidth = measurable.minIntrinsicWidth(constraints.minHeight)
                val intrinsicHeight = measurable.minIntrinsicHeight(constraints.minWidth)
                Timber.d("intrinsicsConstraints $intrinsicWidth $intrinsicHeight")
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = 0, minHeight = 0,
                        maxWidth = intrinsicWidth, maxHeight = intrinsicHeight
                    )
                )*/
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            },
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
