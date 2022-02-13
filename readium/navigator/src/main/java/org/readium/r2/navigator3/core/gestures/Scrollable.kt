package org.readium.r2.navigator3.core.gestures

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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

internal fun Modifier.scrollable(
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
        touchScrollImplementation(
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
private fun Modifier.touchScrollImplementation(
    interactionSource: MutableInteractionSource?,
    reverseDirection: Boolean,
    horizontalState: ScrollableState,
    verticalState: ScrollableState,
    flingBehavior: FlingBehavior?,
    enabled: Boolean
): Modifier {
    val fling = flingBehavior ?: ScrollableDefaults.flingBehavior()
    val nestedScrollDispatcher = remember { mutableStateOf(NestedScrollDispatcher()) }

    val scrollLogic = rememberUpdatedState(
        ScrollingLogic(reverseDirection, nestedScrollDispatcher, horizontalState, verticalState, fling)
    )

    val nestedScrollConnection = remember(enabled) {
        scrollableNestedScrollConnection(scrollLogic, enabled)
    }

    val horizontalDraggableState = remember { ScrollDraggableState(scrollLogic, Orientation.Horizontal) }

    val verticalDraggableState = remember { ScrollDraggableState(scrollLogic, Orientation.Vertical) }

    return draggable(
        horizontalDraggableState,
        verticalDraggableState,
        enabled = enabled,
        interactionSource = interactionSource,
        reverseDirection = false,
        startDragImmediately = horizontalState.isScrollInProgress || verticalState.isScrollInProgress,
        onDragStopped = { velocity ->
            val adjustedVelocity = velocity.coerceVelocity()
            nestedScrollDispatcher.value.coroutineScope.launch {
                scrollLogic.value.onDragStopped(adjustedVelocity)
            }
        },
    ).nestedScroll(nestedScrollConnection, nestedScrollDispatcher.value)
}

private fun Velocity.coerceVelocity(): Velocity {
    Timber.d("adjusting velocity $x $y")
    val adjusted = when {
        x == 0f || y == 0f -> this
        abs(x / y) > 100 -> Velocity(x, 0f)
        abs(y / x) > 100 -> Velocity(0f, y)
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
    val reverseDirection: Boolean,
    val nestedScrollDispatcher: State<NestedScrollDispatcher>,
    val horizontalState: ScrollableState,
    val verticalState: ScrollableState,
    val flingBehavior: FlingBehavior
) {
    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun Float.toOffset(orientation: Orientation) =
        if (orientation == Orientation.Horizontal)
            Offset(this, 0f)
        else
            Offset(0f, this)

    fun Offset.toFloat(orientation: Orientation) =
        if (orientation == Orientation.Horizontal)
            x
        else
            y

    fun ScrollScope.dispatchScroll(scrollDelta: Float, source: NestedScrollSource, orientation: Orientation): Float {
        val nestedScrollDispatcher = nestedScrollDispatcher.value
        val preConsumedByParent = nestedScrollDispatcher
            .dispatchPreScroll(scrollDelta.toOffset(orientation), source)

        val scrollAvailable = scrollDelta - preConsumedByParent.toFloat(orientation)
        val consumed = scrollBy(scrollAvailable.reverseIfNeeded()).reverseIfNeeded()
        val leftForParent = scrollAvailable - consumed
        nestedScrollDispatcher.dispatchPostScroll(
            consumed.toOffset(orientation),
            leftForParent.toOffset(orientation),
            source
        )
        return leftForParent
    }

    fun performRawScroll(scroll: Offset): Offset {
        val x = if (horizontalState.isScrollInProgress) {
            0f
        } else {
            horizontalState.dispatchRawDelta(scroll.x.reverseIfNeeded())
                .reverseIfNeeded()
        }
        val y = if (verticalState.isScrollInProgress) {
            0f
        } else {
            verticalState.dispatchRawDelta(scroll.x.reverseIfNeeded())
                .reverseIfNeeded()
        }
        return Offset(x, y)
    }

    fun performRelocationScroll(scroll: Offset): Offset {
        nestedScrollDispatcher.value.coroutineScope.launch {
            launch {
                horizontalState.animateScrollBy(scroll.x.reverseIfNeeded())
            }
            launch {
                verticalState.animateScrollBy(scroll.y.reverseIfNeeded())
            }

        }
        return scroll
    }

    suspend fun onDragStopped(velocity: Velocity) {
        val preConsumedByParent = nestedScrollDispatcher.value.dispatchPreFling(velocity)
        val available = velocity - preConsumedByParent
        val velocityLeft = doFlingAnimation(available)
        nestedScrollDispatcher.value.dispatchPostFling(available - velocityLeft, velocityLeft)
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        val resultX = nestedScrollDispatcher.value.coroutineScope.async {
            doHorizontalFling(available.x)
        }
        val resultY = nestedScrollDispatcher.value.coroutineScope.async {
            doVerticalFling(available.y)
        }
        return Velocity(resultX.await(), resultY.await())
    }

    private suspend fun doHorizontalFling(available: Float): Float {
        var resultX = 0f
        horizontalState.scroll {
            val outerScopeScroll: (Float) -> Float = { delta ->
                delta - this.dispatchScroll(delta.reverseIfNeeded(), NestedScrollSource.Fling, Orientation.Horizontal).reverseIfNeeded()
            }
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return outerScopeScroll.invoke(pixels)
                }
            }
            with(scope) {
                with(flingBehavior) {
                    resultX = performFling(available.reverseIfNeeded())
                        .reverseIfNeeded()
                }
            }
        }
        return resultX
    }

    private suspend fun doVerticalFling(available: Float): Float {
        var resultY = 0f
        verticalState.scroll {
            val outerScopeScroll: (Float) -> Float = { delta ->
                delta - this.dispatchScroll(delta.reverseIfNeeded(), NestedScrollSource.Fling, Orientation.Vertical).reverseIfNeeded()
            }
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return outerScopeScroll.invoke(pixels)
                }
            }
            with(scope) {
                with(flingBehavior) {
                    resultY = performFling(available.reverseIfNeeded())
                        .reverseIfNeeded()
                }
            }
        }
        return resultY
    }
}

private class ScrollDraggableState(
    val scrollLogic: State<ScrollingLogic>,
    val orientation: Orientation
) : DraggableState, DragScope {
    var latestScrollScope: ScrollScope = NoOpScrollScope

    override fun dragBy(pixels: Float) {
        with(scrollLogic.value) {
            with(latestScrollScope) {
                dispatchScroll(pixels, NestedScrollSource.Drag, orientation)
            }
        }
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ) {
        if (orientation == Orientation.Horizontal) {
            scrollLogic.value.horizontalState.scroll(dragPriority) {
                latestScrollScope = this
                block()
            }
        } else {
            scrollLogic.value.verticalState.scroll(dragPriority) {
                latestScrollScope = this
                block()
            }
        }
    }

    override fun dispatchRawDelta(delta: Float) {
        val deltaOffset =
            if (orientation == Orientation.Horizontal) {
                Offset(delta, 0f)
            } else {
                Offset(0f, delta)
            }
        with(scrollLogic.value) { performRawScroll(deltaOffset) }
    }
}

private val NoOpScrollScope: ScrollScope = object : ScrollScope {
    override fun scrollBy(pixels: Float): Float = pixels
}
