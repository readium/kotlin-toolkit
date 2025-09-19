/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package org.readium.navigator.web.internals.gestures

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * A Gesture detector that waits for pointer down and touch slop in the direction specified by
 * [orientationLock] and then calls [onDrag] for each drag event.
 * It follows the touch slop detection of
 * [androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation] but will consume the
 * position change automatically once the touch slop has been crossed, the amount of drag over
 * the touch slop is reported as the first drag event [onDrag] after the slop is crossed.
 * If [shouldAwaitTouchSlop] returns true the touch slop recognition phase will be ignored
 * and the drag gesture will be recognized immediately.The first [onDrag] in this case will report
 * an [Offset.Zero].
 *
 * [onDragStart] is called when the touch slop has been passed and includes an [Offset] representing
 * the last known pointer position relative to the containing element as well as  the initial
 * down event that triggered this gesture detection cycle. The [Offset] can be outside
 * the actual bounds of the element itself meaning the numbers can be negative or larger than the
 * element bounds if the touch target is smaller than the
 * [ViewConfiguration.minimumTouchTargetSize].
 *
 * [onDragEnd] is called after all pointers are up with the event change of the up event
 * and [onDragCancel] is called if another gesture has consumed pointer input,
 * canceling this gesture.
 *
 * @param onDragStart A lambda to be called when the drag gesture starts, it contains information
 * about the last known [PointerInputChange] relative to the containing element and the post slop
 * delta.
 * @param onDragEnd A lambda to be called when the gesture ends. It contains information about the
 * up [PointerInputChange] that finished the gesture.
 * @param onDragCancel A lambda to be called when the gesture is cancelled either by an error or
 * when it was consumed.
 * @param shouldAwaitTouchSlop Indicates if touch slop detection should be skipped.
 * @param orientationLock Optionally locks detection to this orientation, this means, when this is
 * provided, touch slop detection and drag event detection will be conditioned to the given
 * orientation axis. [onDrag] will still dispatch events on with information in both axis, but
 * if orientation lock is provided, only events that happen on the given orientation will be
 * considered. If no value is provided (i.e. null) touch slop and drag detection will happen on
 * an "any" orientation basis, that is, touch slop will be detected if crossed in either direction
 * and drag events will be dispatched if present in either direction.
 * @param onDrag A lambda to be called for each delta event in the gesture. It contains information
 * about the [PointerInputChange] and the movement offset.
 */
internal suspend fun PointerInputScope.detectDragGestures(
    onDragStart: (change: PointerInputChange, initialDelta: Offset) -> Unit,
    onDragEnd: (change: PointerInputChange) -> Unit,
    onDragCancel: () -> Unit,
    shouldAwaitTouchSlop: () -> Boolean,
    orientationLock: Orientation?,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val initialDown =
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val awaitTouchSlop = shouldAwaitTouchSlop()

        if (!awaitTouchSlop) {
            initialDown.consume()
        }
        val down = awaitFirstDown(requireUnconsumed = false)
        var drag: PointerInputChange?
        var overSlop = Offset.Zero
        var initialDelta = Offset.Zero

        if (awaitTouchSlop) {
            do {
                drag = awaitPointerSlopOrCancellation(
                    down.id,
                    down.type,
                    orientation = orientationLock
                ) { change, over ->
                    change.consume()
                    overSlop = over
                }
            } while (drag != null && !drag.isConsumed)
            initialDelta = overSlop
        } else {
            drag = initialDown
        }

        if (drag != null) {
            onDragStart.invoke(drag, initialDelta)
            onDrag(drag, overSlop)
            val upEvent = drag(
                pointerId = drag.id,
                onDrag = {
                    onDrag(it, it.positionChange())
                    it.consume()
                },
                orientation = orientationLock,
                motionConsumed = {
                    it.isConsumed
                }
            )
            if (upEvent == null) {
                onDragCancel()
            } else {
                onDragEnd(upEvent)
            }
        }
    }
}

/**
 * Continues to read drag events until all pointers are up or the drag event is canceled.
 * The initial pointer to use for driving the drag is [pointerId]. [onDrag] is called
 * whenever the pointer moves. The up event is returned at the end of the drag gesture.
 *
 * @param pointerId The pointer where that is driving the gesture.
 * @param onDrag Callback for every new drag event.
 * @param motionConsumed If the PointerInputChange should be considered as consumed.
 *
 * @return The last pointer input event change when gesture ended with all pointers up
 * and null when the gesture was canceled.
 */
internal suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
    orientation: Orientation?,
    motionConsumed: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) {
            val positionChange = it.positionChangeIgnoreConsumed()
            val motionChange = if (orientation == null) {
                positionChange.getDistance()
            } else {
                if (orientation == Orientation.Vertical) positionChange.y else positionChange.x
            }
            motionChange != 0.0f
        } ?: return null

        if (motionConsumed(change)) {
            return null
        }

        if (change.changedToUpIgnoreConsumed()) {
            return change
        }

        onDrag(change)
        pointer = change.id
    }
}

/**
 * Waits for a single drag in one axis, final pointer up, or all pointers are up.
 * When [pointerId] has lifted, another pointer that is down is chosen to be the finger
 * governing the drag. When the final pointer is lifted, that [PointerInputChange] is
 * returned. When a drag is detected, that [PointerInputChange] is returned. A drag is
 * only detected when [hasDragged] returns `true`.
 *
 * `null` is returned if there was an error in the pointer input stream and the pointer
 * that was down was dropped before the 'up' was received.
 */
private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
    hasDragged: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else if (hasDragged(dragEvent)) {
            return dragEvent
        }
    }
}

/**
 * Waits for drag motion and uses [orientation] to detect the direction of  touch slop detection.
 * It passes [pointerId] as the pointer to examine. If [pointerId] is raised, another pointer from
 * those that are down will be chosen to
 * lead the gesture, and if none are down, `null` is returned. If [pointerId] is not down when
 * [awaitPointerSlopOrCancellation] is called, then `null` is returned.
 *
 * When pointer slop is detected, [onPointerSlopReached] is called with the change and the distance
 * beyond the pointer slop. If [onPointerSlopReached] does not consume the
 * position change, pointer slop will not have been considered detected and the detection will
 * continue or, if it is consumed, the [PointerInputChange] that was consumed will be returned.
 *
 * This works with [androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation]
 * for the other axis to ensure that only horizontal
 * or vertical dragging is done, but not both. It also works for dragging in two ways when using
 * [androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation]
 *
 * @return The [PointerInputChange] of the event that was consumed in [onPointerSlopReached] or
 * `null` if all pointers are raised or the position change was consumed by another gesture
 * detector.
 */
private suspend inline fun AwaitPointerEventScope.awaitPointerSlopOrCancellation(
    pointerId: PointerId,
    pointerType: PointerType,
    orientation: Orientation?,
    onPointerSlopReached: (PointerInputChange, Offset) -> Unit,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val touchSlop = viewConfiguration.pointerSlop(pointerType)
    var pointer: PointerId = pointerId
    val touchSlopDetector = TouchSlopDetector(orientation)
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.isConsumed) {
            return null
        } else if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return null
            } else {
                pointer = otherDown.id
            }
        } else {
            val postSlopOffset = touchSlopDetector.addPointerInputChange(dragEvent, touchSlop)
            if (postSlopOffset != null) {
                onPointerSlopReached(
                    dragEvent,
                    postSlopOffset
                )
                if (dragEvent.isConsumed) {
                    return dragEvent
                } else {
                    touchSlopDetector.reset()
                }
            } else {
                // verify that nothing else consumed the drag event
                awaitPointerEvent(PointerEventPass.Final)
                if (dragEvent.isConsumed) {
                    return null
                }
            }
        }
    }
}

/**
 * Detects if touch slop has been crossed after adding a series of [PointerInputChange].
 * For every new [PointerInputChange] one should add it to this detector using
 * [addPointerInputChange]. If the position change causes the touch slop to be crossed,
 * [addPointerInputChange] will return true.
 */
private class TouchSlopDetector(val orientation: Orientation? = null) {

    fun Offset.mainAxis() = if (orientation == Orientation.Horizontal) x else y
    fun Offset.crossAxis() = if (orientation == Orientation.Horizontal) y else x

    /**
     * The accumulation of drag deltas in this detector.
     */
    private var totalPositionChange: Offset = Offset.Zero

    /**
     * Adds [dragEvent] to this detector. If the accumulated position changes crosses the touch
     * slop provided by [touchSlop], this method will return the post slop offset, that is the
     * total accumulated delta change minus the touch slop value, otherwise this should return null.
     */
    fun addPointerInputChange(
        dragEvent: PointerInputChange,
        touchSlop: Float,
    ): Offset? {
        val currentPosition = dragEvent.position
        val previousPosition = dragEvent.previousPosition
        val positionChange = currentPosition - previousPosition
        totalPositionChange += positionChange

        val inDirection = if (orientation == null) {
            totalPositionChange.getDistance()
        } else {
            totalPositionChange.mainAxis().absoluteValue
        }

        val hasCrossedSlop = inDirection >= touchSlop

        return if (hasCrossedSlop) {
            calculatePostSlopOffset(touchSlop)
        } else {
            null
        }
    }

    /**
     * Resets the accumulator associated with this detector.
     */
    fun reset() {
        totalPositionChange = Offset.Zero
    }

    private fun calculatePostSlopOffset(touchSlop: Float): Offset {
        return if (orientation == null) {
            val touchSlopOffset =
                totalPositionChange / totalPositionChange.getDistance() * touchSlop
            // update postSlopOffset
            totalPositionChange - touchSlopOffset
        } else {
            val finalMainAxisChange = totalPositionChange.mainAxis() -
                (sign(totalPositionChange.mainAxis()) * touchSlop)
            val finalCrossAxisChange = totalPositionChange.crossAxis()
            if (orientation == Orientation.Horizontal) {
                Offset(finalMainAxisChange, finalCrossAxisChange)
            } else {
                Offset(finalCrossAxisChange, finalMainAxisChange)
            }
        }
    }
}

private fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.fastFirstOrNull { it.id == pointerId }?.pressed != true

// This value was determined using experiments and common sense.
// We can't use zero slop, because some hypothetical desktop/mobile devices can send
// pointer events with a very high precision (but I haven't encountered any that send
// events with less than 1px precision)
private val mouseSlop = 0.125.dp
private val defaultTouchSlop = 18.dp // The default touch slop on Android devices
private val mouseToTouchSlopRatio = mouseSlop / defaultTouchSlop

// TODO(demin): consider this as part of ViewConfiguration class after we make *PointerSlop*
//  functions public (see the comment at the top of the file).
//  After it will be a public API, we should get rid of `touchSlop / 144` and return absolute
//  value 0.125.dp.toPx(). It is not possible right now, because we can't access density.
internal fun ViewConfiguration.pointerSlop(pointerType: PointerType): Float {
    return when (pointerType) {
        PointerType.Mouse -> touchSlop * mouseToTouchSlopRatio
        else -> touchSlop
    }
}
