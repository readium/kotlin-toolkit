package org.readium.r2.navigator3.gestures

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.zoomable(state: MutableState<Float>): Modifier =
    this.pointerInput(Unit) {
        detectTransformGestures(
            onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                val oldScale = state.value
                val newScale = (oldScale * gestureZoom).coerceAtLeast(1f)

                // For natural zooming and rotating, the centroid of the gesture should
                // be the fixed point where zooming and rotating occurs.
                // We compute where the centroid was (in the pre-transformed coordinate
                // space), and then compute where it will be after this delta.
                // We then compute what the new offset should be to keep the centroid
                // visually stationary for rotating and zooming, and also apply the pan.
                /*state.offsetState.value = (state.offsetState.value + centroid / oldScale) -
                        (centroid / newScale)*/
                val newCentroid = centroid * newScale / oldScale
                val delta = newCentroid - centroid
                //state.oppositeScrollState.dispatchRawDelta(centroid.x)
                //state.lazyListState.dispatchRawDelta(centroid.y)
                state.value = newScale
            }
        )
    }