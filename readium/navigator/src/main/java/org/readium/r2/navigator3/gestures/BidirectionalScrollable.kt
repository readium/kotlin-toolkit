package org.readium.r2.navigator3.gestures

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

internal fun Modifier.bidirectionalScrollable(
    horizontalState: ScrollableState,
    verticalState: ScrollableState,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollable"
        properties["horizontalState"] = horizontalState
        properties["verticalState"] = verticalState
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
    },
    factory = {
        bidirectionalTouchScrollImplementation(
            interactionSource,
            reverseDirection,
            horizontalState,
            verticalState,
            flingBehavior,
            enabled
        )
    }
)

@Suppress("ComposableModifierFactory")
@Composable
private fun Modifier.bidirectionalTouchScrollImplementation(
    interactionSource: MutableInteractionSource?,
    reverseDirection: Boolean,
    horizontalState: ScrollableState,
    verticalState: ScrollableState,
    flingBehavior: FlingBehavior?,
    enabled: Boolean
): Modifier {
    val fling = flingBehavior ?: ScrollableDefaults.flingBehavior()
    val nestedScrollDispatcher = remember { mutableStateOf(NestedScrollDispatcher()) }

    val horizontalScrollLogic = rememberUpdatedState(
        ScrollingLogic(Orientation.Horizontal, reverseDirection, nestedScrollDispatcher, horizontalState, fling)
    )
    val horizontalNestedScrollConnection = remember(enabled) {
        scrollableNestedScrollConnection(horizontalScrollLogic, enabled)
    }
    val horizontalDraggableState = remember { ScrollDraggableState(horizontalScrollLogic) }

    //val verticalNestedScrollDispatcher = remember { mutableStateOf(NestedScrollDispatcher()) }
    val verticalScrollLogic = rememberUpdatedState(
        ScrollingLogic(Orientation.Vertical, reverseDirection, nestedScrollDispatcher, verticalState, fling)
    )
    val verticalNestedScrollConnection = remember(enabled) {
        scrollableNestedScrollConnection(verticalScrollLogic, enabled)
    }
    val verticalDraggableState = remember { ScrollDraggableState(verticalScrollLogic) }

    return bidirectionalDraggable(
        horizontalDraggableState,
        verticalDraggableState,
        enabled = enabled,
        interactionSource = interactionSource,
        reverseDirection = false,
        startDragImmediately = horizontalState.isScrollInProgress || verticalState.isScrollInProgress,
        onDragStopped = { velocity ->
            val adjustedVelocity = velocity.coerceVelocity()
            nestedScrollDispatcher.value.coroutineScope.launch {
                launch {
                    horizontalScrollLogic.value.onDragStopped(adjustedVelocity.x)
                }

                launch {
                    verticalScrollLogic.value.onDragStopped(adjustedVelocity.y)
                }
            }
            /*verticalNestedScrollDispatcher.value.coroutineScope.launch {
                verticalScrollLogic.value.onDragStopped(adjustedVelocity.y)
            }*/
        },
    ).nestedScroll(horizontalNestedScrollConnection, nestedScrollDispatcher.value)
        //.nestedScroll(verticalNestedScrollConnection, nestedScrollDispatcher.value)
}

private fun Velocity.coerceVelocity(): Velocity {
    Timber.d("adjusting velocity $x $y")
    val adjusted = when {
        x == 0f || y == 0f -> this
        abs(x / y) > 3 -> Velocity(x, 0f)
        abs(y / x) > 3 -> Velocity(0f, y)
        else -> this
    }
    Timber.d("adjusted velocity ${adjusted.x} ${adjusted.y}")
    return adjusted
}


private fun scrollableNestedScrollConnection(
    scrollLogic: State<ScrollingLogic>,
    enabled: Boolean
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = if (enabled) {
        @Suppress("DEPRECATION")
        when (source) {
            NestedScrollSource.Drag, NestedScrollSource.Fling -> scrollLogic.value.performRawScroll(available)
            @OptIn(ExperimentalComposeUiApi::class)
            (NestedScrollSource.Relocate) -> scrollLogic.value.performRelocationScroll(available)
            else -> error("$source scroll not supported.")
        }
    } else {
        Offset.Zero
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        return if (enabled) {
            val velocityLeft = scrollLogic.value.doFlingAnimation(available)
            available - velocityLeft
        } else {
            Velocity.Zero
        }
    }
}

private class ScrollingLogic(
    val orientation: Orientation,
    val reverseDirection: Boolean,
    val nestedScrollDispatcher: State<NestedScrollDispatcher>,
    val scrollableState: ScrollableState,
    val flingBehavior: FlingBehavior
) {
    fun Float.toOffset(): Offset = when {
        this == 0f -> Offset.Zero
        orientation == Orientation.Horizontal -> Offset(this, 0f)
        else -> Offset(0f, this)
    }

    fun Float.toVelocity(): Velocity =
        if (orientation == Orientation.Horizontal) Velocity(this, 0f) else Velocity(0f, this)

    fun Offset.toFloat(): Float =
        if (orientation == Orientation.Horizontal) this.x else this.y

    fun Velocity.toFloat(): Float =
        if (orientation == Orientation.Horizontal) this.x else this.y

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun ScrollScope.dispatchScroll(scrollDelta: Float, source: NestedScrollSource): Float {
        val nestedScrollDispatcher = nestedScrollDispatcher.value
        val preConsumedByParent = nestedScrollDispatcher
            .dispatchPreScroll(scrollDelta.toOffset(), source)

        val scrollAvailable = scrollDelta - preConsumedByParent.toFloat()
        val consumed = scrollBy(scrollAvailable.reverseIfNeeded()).reverseIfNeeded()
        val leftForParent = scrollAvailable - consumed
        nestedScrollDispatcher.dispatchPostScroll(
            consumed.toOffset(),
            leftForParent.toOffset(),
            source
        )
        return leftForParent
    }

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            scrollableState.dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
                .reverseIfNeeded().toOffset()
        }
    }

    fun performRelocationScroll(scroll: Offset): Offset {
        nestedScrollDispatcher.value.coroutineScope.launch {
            scrollableState.animateScrollBy(scroll.toFloat().reverseIfNeeded())
        }
        return scroll
    }

    suspend fun onDragStopped(axisVelocity: Float) {
        val velocity = axisVelocity.toVelocity()
        val preConsumedByParent = nestedScrollDispatcher.value.dispatchPreFling(velocity)
        val available = velocity - preConsumedByParent
        val velocityLeft = doFlingAnimation(available)
        nestedScrollDispatcher.value.dispatchPostFling(available - velocityLeft, velocityLeft)
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        scrollableState.scroll {
            val outerScopeScroll: (Float) -> Float = { delta ->
                delta - this.dispatchScroll(delta.reverseIfNeeded(), NestedScrollSource.Fling).reverseIfNeeded()
            }
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return outerScopeScroll.invoke(pixels)
                }
            }
            with(scope) {
                with(flingBehavior) {
                    result = performFling(available.toFloat().reverseIfNeeded())
                        .reverseIfNeeded().toVelocity()
                }
            }
        }
        return result
    }
}

private class ScrollDraggableState(
    val scrollLogic: State<ScrollingLogic>
) : DraggableState, DragScope {
    var latestScrollScope: ScrollScope = NoOpScrollScope

    override fun dragBy(pixels: Float) {
        with(scrollLogic.value) {
            with(latestScrollScope) {
                dispatchScroll(pixels, NestedScrollSource.Drag)
            }
        }
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ) {
        scrollLogic.value.scrollableState.scroll(dragPriority) {
            latestScrollScope = this
            block()
        }
    }

    override fun dispatchRawDelta(delta: Float) {
        with(scrollLogic.value) { performRawScroll(delta.toOffset()) }
    }
}

private val NoOpScrollScope: ScrollScope = object : ScrollScope {
    override fun scrollBy(pixels: Float): Float = pixels
}
