package org.readium.r2.navigator3.gestures

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
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

                                state.scaleState.value = newScale

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

    val horizontalScrollState: ScrollableState

    val verticalScrollState: ScrollableState
}