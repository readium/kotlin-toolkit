package org.readium.r2.navigator3.lazylist

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
internal fun ZoomableLazyList(
    direction: Direction,
    modifier: Modifier = Modifier,
    state: ZoomableLazyListState = rememberZoomableLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit
) {
    DirectedLazyList(
        modifier = modifier
            .graphicsLayer(
                scaleX = state.scale,
                scaleY = state.scale,
                //translationX = state.offsetState.value.x,
                //translationY = state.offsetState.value.y,
                transformOrigin = TransformOrigin(0f, 0f)
            )
            .transformable(
                state = state.transformableState,
                lockRotationOnZoomPan = true
            )
            /*.pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        val oldScale = state.scale
                        val newScale = (state.scale * gestureZoom).coerceAtLeast(1f)

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
                        state.scaleState.value = newScale
                    }
                )
            }*/
            .horizontalScroll(state.oppositeScrollState),
        state = state.lazyListState,
        contentPadding = contentPadding,
        direction = direction,
        content = content
    )
}

internal class ZoomableLazyListState(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    oppositeDirectionScrollOffset: Int = 0,
    scale: Float = 1f,
) {
   var scaleState: MutableState<Float> =
        mutableStateOf(scale)

    var rotationState = mutableStateOf(0f)
    var offsetState =  mutableStateOf(Offset.Zero)

    fun Offset.coerceAtLeast(minimumValues: Offset) =
        Offset(this.x.coerceAtLeast(minimumValue = minimumValues.x), this.y.coerceAtLeast(minimumValues.y))

    internal val transformableState = TransformableState { zoomChange, panChange, rotationChange ->
        scaleState.value = (scaleState.value * zoomChange).coerceAtLeast(1f)
        offsetState.value = (offsetState.value + panChange).coerceAtLeast(Offset.Zero)
    }

    internal val lazyListState: LazyListState =
        LazyListState(firstVisibleItemIndex, firstVisibleItemScrollOffset)

    internal val oppositeScrollState: ScrollState =
        ScrollState(oppositeDirectionScrollOffset)

    internal val scale: Float
        get() = scaleState.value

    companion object {
        val Saver: Saver<ZoomableLazyListState, *> = listSaver(
            save = {
                listOf(
                    it.lazyListState.firstVisibleItemIndex,
                    it.lazyListState.firstVisibleItemScrollOffset,
                    it.oppositeScrollState.value,
                    it.scale
                )
            },
            restore = {
                ZoomableLazyListState(
                    firstVisibleItemIndex = it[0] as Int,
                    firstVisibleItemScrollOffset = it[1] as Int,
                    oppositeDirectionScrollOffset = it[2] as Int,
                    scale = it[3] as Float
                )
            }
        )
    }
}

@Composable
internal fun rememberZoomableLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialOppositeDirectionScrollOffset: Int = 0,
    initialScale: Float = 1f,
): ZoomableLazyListState {
    return rememberSaveable(saver = ZoomableLazyListState.Saver) {
        ZoomableLazyListState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
            initialOppositeDirectionScrollOffset,
            initialScale
        )
    }
}