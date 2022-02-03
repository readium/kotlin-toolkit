package org.readium.r2.navigator3.gestures

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import timber.log.Timber
import kotlin.math.abs

internal fun Modifier.zoomable(state: ZoomableState): Modifier =
    this.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                var zoom = 1f
                var pastTouchSlop = false
                val touchSlop = viewConfiguration.touchSlop

                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val canceled = event.changes.fastAny { it.positionChangeConsumed() }
                    if (!canceled) {
                        val zoomChange = event.calculateZoom()

                        if (!pastTouchSlop) {
                            zoom *= zoomChange

                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = abs(1 - zoom) * centroidSize

                            if (zoomMotion > touchSlop
                            ) {
                                pastTouchSlop = true
                            }
                        }

                        if (pastTouchSlop) {
                            val centroid = event.calculateCentroid(useCurrent = false)
                            if (zoomChange != 1f) {
                                val oldScale = state.scaleState.value
                                val newScale = oldScale * zoomChange
                                Timber.d("zoom changed from to $oldScale to $newScale")

                                // For natural zooming, the centroid of the gesture should
                                // be the fixed point where zooming, occurs.
                                // We compute where the centroid was (in the pre-transformed coordinate
                                // space), and then compute where it will be after this delta.
                                // We then scroll to the new offset to keep the centroid
                                // visually stationary for zooming.
                                /*state.offsetState.value = (state.offsetState.value + centroid / oldScale) -
                                        (centroid / newScale)*/
                                val newCentroid = centroid * newScale / oldScale
                                Timber.d("centroid changed from $centroid to $newCentroid")
                                state.scaleState.value = newScale
                                state.onScaleChanged(zoomChange, centroid)

                                event.changes.fastForEach {
                                    if (it.positionChanged()) {
                                        it.consumeAllChanges()
                                    }
                                }
                            }
                        }
                    }
                } while (!canceled && event.changes.fastAny { it.pressed })
            }
        }
    }

internal interface ZoomableState {

    var scaleState: MutableState<Float>

    fun onScaleChanged(zoomChange: Float, centroid: Offset)
}