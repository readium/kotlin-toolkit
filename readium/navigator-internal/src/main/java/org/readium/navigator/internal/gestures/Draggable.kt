package org.readium.navigator.internal.gestures

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

internal fun Modifier.draggable(
    horizontalState: DraggableState,
    verticalState: DraggableState,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    startDragImmediately: Boolean = false,
    onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {},
    onDragStopped: suspend CoroutineScope.(velocity: Velocity) -> Unit = {},
    reverseDirection: Boolean = false
): Modifier = this.draggable(
    horizontalState = horizontalState,
    verticalState = verticalState,
    enabled = enabled,
    interactionSource = interactionSource,
    startDragImmediately = { startDragImmediately },
    onDragStarted = onDragStarted,
    onDragStopped = onDragStopped,
    reverseDirection = reverseDirection,
    canDrag = { true }
)

private fun Modifier.draggable(
    horizontalState: DraggableState,
    verticalState: DraggableState,
    canDrag: (PointerInputChange) -> Boolean,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    startDragImmediately: () -> Boolean,
    onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {},
    onDragStopped: suspend CoroutineScope.(velocity: Velocity) -> Unit = {},
    reverseDirection: Boolean = false
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "draggable"
        properties["canDrag"] = canDrag
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["interactionSource"] = interactionSource
        properties["startDragImmediately"] = startDragImmediately
        properties["onDragStarted"] = onDragStarted
        properties["onDragStopped"] = onDragStopped
        properties["horizontalState"] = horizontalState
        properties["verticalState"] = verticalState
    }
) {
    val draggedInteraction = remember { mutableStateOf<DragInteraction.Start?>(null) }
    DisposableEffect(interactionSource) {
        onDispose {
            draggedInteraction.value?.let { interaction ->
                interactionSource?.tryEmit(DragInteraction.Cancel(interaction))
                draggedInteraction.value = null
            }
        }
    }
    val channel = remember { Channel<DragEvent>(capacity = Channel.UNLIMITED) }
    val startImmediatelyState = rememberUpdatedState(startDragImmediately)
    val canDragState = rememberUpdatedState(canDrag)
    val dragLogic by rememberUpdatedState(
        DragLogic(onDragStarted, onDragStopped, draggedInteraction, interactionSource)
    )
    LaunchedEffect(horizontalState, verticalState) {
        while (isActive) {
            var event = channel.receive()
            if (event !is DragEvent.DragStarted) continue
            with(dragLogic) { processDragStart(event as DragEvent.DragStarted) }
            try {
                horizontalState.drag(MutatePriority.UserInput) {
                    val horizontalScope = this
                    verticalState.drag(MutatePriority.UserInput) {
                        val verticalScope = this
                        while (event !is DragEvent.DragStopped && event !is DragEvent.DragCancelled) {
                            (event as? DragEvent.DragDelta)?.let {
                                horizontalScope.dragBy(it.delta.x)
                                verticalScope.dragBy(it.delta.y)
                            }
                            event = channel.receive()
                        }
                    }
                }
                with(dragLogic) {
                    if (event is DragEvent.DragStopped) {
                        processDragStop(event as DragEvent.DragStopped)
                    } else if (event is DragEvent.DragCancelled) {
                        processDragCancel()
                    }
                }
            } catch (c: CancellationException) {
                with(dragLogic) { processDragCancel() }
            }
        }
    }
    Modifier.pointerInput(enabled, reverseDirection) {
        if (!enabled) return@pointerInput
        coroutineScope {
            forEachGesture {
                awaitPointerEventScope {
                    val velocityTracker = VelocityTracker()
                    awaitDownAndSlop(canDragState, startImmediatelyState)?.let {
                        var isDragSuccessful = false
                        try {
                            isDragSuccessful = awaitDrag(
                                it,
                                velocityTracker,
                                channel,
                                reverseDirection,
                            )
                        } catch (cancellation: CancellationException) {
                            isDragSuccessful = false
                            if (!isActive) throw cancellation
                        } finally {
                            val event = if (isDragSuccessful) {
                                val velocity =
                                    velocityTracker.calculateVelocity()
                                DragEvent.DragStopped(velocity * if (reverseDirection) -1.0f else 1.0f)
                            } else {
                                DragEvent.DragCancelled
                            }
                            channel.trySend(event)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitDrag(
    dragStart: Pair<PointerInputChange, Offset>,
    velocityTracker: VelocityTracker,
    channel: SendChannel<DragEvent>,
    reverseDirection: Boolean,
): Boolean {
    val initialDelta = dragStart.second
    val startEvent = dragStart.first
    velocityTracker.addPosition(startEvent.uptimeMillis, startEvent.position)

    val adjustedStart = startEvent.position - initialDelta
    channel.trySend(DragEvent.DragStarted(adjustedStart))

    channel.trySend(DragEvent.DragDelta(if (reverseDirection) initialDelta * -1.0f else initialDelta))

    val dragTick: (PointerInputChange) -> Unit = { event: PointerInputChange ->
        velocityTracker.addPosition(event.uptimeMillis, event.position)
        val delta = event.positionChange()
        event.consumePositionChange()
        channel.trySend(DragEvent.DragDelta(if (reverseDirection) delta * -1.0f else delta))
    }
    return drag(startEvent.id, dragTick)
}


/**
 * Continues to read drag events until all pointers are up or the drag event is canceled.
 * The initial pointer to use for driving the drag is [pointerId]. [motionFromChange]
 * converts the [PointerInputChange] to the pixel change in the direction that this
 * drag should detect. [onDrag] is called whenever the pointer moves and [motionFromChange]
 * returns non-zero.
 *
 * @return `true` when the gesture ended with all pointers up and `false` when the gesture
 * was canceled.
 */
private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): Boolean {
    if (currentEvent.isPointerUp(pointerId)) {
        return false // The pointer has already been lifted, so the gesture is canceled
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) { it.positionChangeIgnoreConsumed() != Offset.Zero }

        if (change.positionChangeConsumed()) {
            return false
        }

        if (change.changedToUpIgnoreConsumed()) {
            return true
        }

        onDrag(change)
        pointer = change.id
    }
}

private fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.fastFirstOrNull { it.id == pointerId }?.pressed != true

/**
 * Waits for a single drag in one axis, final pointer up, or all pointers are up.
 * When [pointerId] has lifted, another pointer that is down is chosen to be the finger
 * governing the drag. When the final pointer is lifted, that [PointerInputChange] is
 * returned. When a drag is detected, that [PointerInputChange] is returned. A drag is
 * only detected when [hasDragged] returns `true`.
 */
private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
    hasDragged: (PointerInputChange) -> Boolean
): PointerInputChange {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer }!!
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

private suspend fun AwaitPointerEventScope.awaitDownAndSlop(
    canDrag: State<(PointerInputChange) -> Boolean>,
    startDragImmediately: State<() -> Boolean>,
): Pair<PointerInputChange, Offset>? {
    val down = awaitFirstDown(requireUnconsumed = false)
    return if (!canDrag.value.invoke(down)) {
        null
    } else if (startDragImmediately.value.invoke()) {
        // since we start immediately we don't wait for slop and the initial delta is 0
        down to Offset.Zero
    } else {
        var initialDelta = Offset.Zero
        val postTouchSlop = { event: PointerInputChange, offset: Offset ->
            event.consumePositionChange()
            initialDelta = offset
        }

        val afterSlopResult = awaitTouchSlopOrCancellation(down.id, postTouchSlop)
        if (afterSlopResult != null) afterSlopResult to initialDelta else null
    }
}

private sealed class DragEvent {
    class DragStarted(val startPoint: Offset) : DragEvent()
    class DragStopped(val velocity: Velocity) : DragEvent()
    object DragCancelled : DragEvent()
    class DragDelta(val delta: Offset) : DragEvent()
}

private class DragLogic(
    val onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    val onDragStopped: suspend CoroutineScope.(velocity: Velocity) -> Unit,
    val dragStartInteraction: MutableState<DragInteraction.Start?>,
    val interactionSource: MutableInteractionSource?
) {

    suspend fun CoroutineScope.processDragStart(event: DragEvent.DragStarted) {
        dragStartInteraction.value?.let { oldInteraction ->
            interactionSource?.emit(DragInteraction.Cancel(oldInteraction))
        }
        val interaction = DragInteraction.Start()
        interactionSource?.emit(interaction)
        dragStartInteraction.value = interaction
        onDragStarted.invoke(this, event.startPoint)
    }

    suspend fun CoroutineScope.processDragStop(event: DragEvent.DragStopped) {
        dragStartInteraction.value?.let { interaction ->
            interactionSource?.emit(DragInteraction.Stop(interaction))
            dragStartInteraction.value = null
        }
        onDragStopped.invoke(this, event.velocity)
    }

    suspend fun CoroutineScope.processDragCancel() {
        dragStartInteraction.value?.let { interaction ->
            interactionSource?.emit(DragInteraction.Cancel(interaction))
            dragStartInteraction.value = null
        }
        onDragStopped.invoke(this, Velocity.Zero)
    }
}
