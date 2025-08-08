/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

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

package org.readium.navigator.web.internals.gestures

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Configure touch scrolling and flinging for the UI element in one or both [Orientation].
 *
 * Users should update their state themselves using default [Scrollable2DState] and its
 * `consumeScrollDelta` callback or by implementing [Scrollable2DState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable2D].
 *
 * @param state [Scrollable2DState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling or null for both orientations
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [Scrollable2DDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * drag events when this scrollable is being dragged.
 *
 * WARNING: Unlike the scrollable modifier and to make it more predictable, nested scroll is not
 * triggered while performing fling: preScroll and postScroll connection' methods do not get called
 * during the fling, only preFling beforehand and postFling afterwards.
 */
@Stable
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.scrollable2D(
    state: Scrollable2DState,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: Fling2DBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    orientation: Orientation? = null,
): Modifier = scrollable2D(
    state = state,
    enabled = enabled,
    reverseDirection = reverseDirection,
    flingBehavior = flingBehavior,
    interactionSource = interactionSource,
    overscrollEffect = null,
    orientation = orientation
)

/**
 * Configure touch scrolling and flinging for the UI element in one or both [Orientation].
 *
 * Users should update their state themselves using default [Scrollable2DState] and its
 * `consumeScrollDelta` callback or by implementing [Scrollable2DState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable2D].
 *
 * This overload provides the access to [OverscrollEffect] that defines the behaviour of the
 * over scrolling logic. Consider using [Scrollable2DDefaults.overscrollEffect] for the platform
 * look-and-feel.
 *
 * @param state [Scrollable2DState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling or null for both orientations.
 * @param overscrollEffect effect to which the deltas will be fed when the scrollable have
 * some scrolling delta left. Pass `null` for no overscroll. If you pass an effect you should
 * also apply [androidx.compose.foundation.overscroll] modifier.
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [Scrollable2DDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * drag events when this scrollable is being dragged.
 */
@Stable
@ExperimentalFoundationApi
internal fun Modifier.scrollable2D(
    state: Scrollable2DState,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: Fling2DBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    orientation: Orientation? = null,
) = this then Scrollable2DElement(
    state,
    overscrollEffect,
    enabled,
    reverseDirection,
    flingBehavior,
    interactionSource,
    orientation
)

@OptIn(ExperimentalFoundationApi::class)
private class Scrollable2DElement(
    val state: Scrollable2DState,
    val overscrollEffect: OverscrollEffect?,
    val enabled: Boolean,
    val reverseDirection: Boolean,
    val flingBehavior: Fling2DBehavior?,
    val interactionSource: MutableInteractionSource?,
    val orientation: Orientation?,
) : ModifierNodeElement<Scrollable2DNode>() {
    override fun create(): Scrollable2DNode {
        return Scrollable2DNode(
            state,
            overscrollEffect,
            flingBehavior,
            enabled,
            reverseDirection,
            interactionSource,
            orientation
        )
    }

    override fun update(node: Scrollable2DNode) {
        node.update(
            state,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            orientation
        )
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is Scrollable2DElement) return false

        if (state != other.state) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (enabled != other.enabled) return false
        if (reverseDirection != other.reverseDirection) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false
        if (orientation != other.orientation) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollable"
        properties["state"] = state
        properties["overscrollEffect"] = overscrollEffect
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
        properties["orientation"] = orientation
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class Scrollable2DNode(
    state: Scrollable2DState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: Fling2DBehavior?,
    enabled: Boolean,
    reverseDirection: Boolean,
    interactionSource: MutableInteractionSource?,
    private var orientation: Orientation?,
) : DragGestureNode(
    canDrag = CanDragCalculation,
    enabled = enabled,
    interactionSource = interactionSource,
    orientation = orientation
),
    ObserverModifierNode,
    CompositionLocalConsumerModifierNode,
    FocusPropertiesModifierNode,
    SemanticsModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    private val nestedScrollDispatcher = NestedScrollDispatcher()

    private val scrollableContainerNode =
        delegate(ScrollableContainerNode(enabled))

    // Place holder fling behavior, we'll initialize it when the density is available.
    private val defaultFlingBehavior = DefaultFling2DBehavior(splineBasedDecay(UnityDensity))

    private val scrollingLogic = ScrollingLogic(
        scrollableState = state,
        overscrollEffect = overscrollEffect,
        reverseDirection = reverseDirection,
        flingBehavior = flingBehavior ?: defaultFlingBehavior,
        nestedScrollDispatcher = nestedScrollDispatcher
    )

    private val nestedScrollConnection =
        ScrollableNestedScrollConnection(enabled = enabled, scrollingLogic = scrollingLogic)

    // Need to wait until onAttach to read the scroll config. Currently this is static, so we
    // don't need to worry about observation / updating this over time.
    private var scrollConfig: ScrollConfig? = null
    private var scrollByAction: ((x: Float, y: Float) -> Boolean)? = null
    private var scrollByOffsetAction: (suspend (Offset) -> Offset)? = null

    init {
        /**
         * Nested scrolling
         */
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))

        /**
         * Focus scrolling
         */
        delegate(FocusTargetModifierNode())
    }

    override suspend fun drag(
        forEachDelta: suspend ((dragDelta: DragEvent.DragDelta) -> Unit) -> Unit,
    ) {
        with(scrollingLogic) {
            scroll(scrollPriority = MutatePriority.UserInput) {
                forEachDelta {
                    val directionalDelta =
                        when (orientation) {
                            null -> it.delta
                            Orientation.Horizontal -> it.delta.copy(y = 0f)
                            Orientation.Vertical -> it.delta.copy(x = 0f)
                        }
                    scrollByWithOverscroll(
                        directionalDelta,
                        source = NestedScrollSource.UserInput
                    )
                }
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {}

    override fun onDragStopped(velocity: Velocity) {
        val directionalVelocity =
            when (orientation) {
                null -> velocity
                Orientation.Horizontal -> velocity.copy(y = 0f)
                Orientation.Vertical -> velocity.copy(x = 0f)
            }
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onDragStopped(directionalVelocity)
        }
    }

    override fun startDragImmediately(): Boolean {
        return scrollingLogic.shouldScrollImmediately()
    }

    fun update(
        state: Scrollable2DState,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseDirection: Boolean,
        flingBehavior: Fling2DBehavior?,
        interactionSource: MutableInteractionSource?,
        orientationLock: Orientation?,
    ) {
        var shouldInvalidateSemantics = false
        if (this.enabled != enabled) { // enabled changed
            nestedScrollConnection.enabled = enabled
            scrollableContainerNode.update(enabled)
            shouldInvalidateSemantics = true
        }
        // a new fling behavior was set, change the resolved one.
        val resolvedFlingBehavior = flingBehavior ?: defaultFlingBehavior

        val resetPointerInputHandling = scrollingLogic.update(
            scrollableState = state,
            overscrollEffect = overscrollEffect,
            reverseDirection = reverseDirection,
            flingBehavior = resolvedFlingBehavior,
            nestedScrollDispatcher = nestedScrollDispatcher
        )

        this.overscrollEffect = overscrollEffect
        this.flingBehavior = flingBehavior
        this.orientation = orientationLock

        // update DragGestureNode
        update(
            canDrag = CanDragCalculation,
            enabled = enabled,
            interactionSource = interactionSource,
            orientationLock = orientationLock,
            shouldResetPointerInputHandling = resetPointerInputHandling
        )

        if (shouldInvalidateSemantics) {
            clearScrollSemanticsActions()
            invalidateSemantics()
        }
    }

    override fun onAttach() {
        updateDefaultFlingBehavior()
        scrollConfig = platformScrollConfig()
    }

    override fun onObservedReadsChanged() {
        // if density changes, update the default fling behavior.
        updateDefaultFlingBehavior()
    }

    private fun updateDefaultFlingBehavior() {
        // monitor change in Density
        observeReads {
            val density = currentValueOf(LocalDensity)
            defaultFlingBehavior.flingDecay = splineBasedDecay(density)
        }
    }

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.canFocus = false
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        super.onPointerEvent(pointerEvent, pass, bounds)
        if (pass == PointerEventPass.Main && pointerEvent.type == PointerEventType.Scroll) {
            processMouseWheelEvent(pointerEvent, bounds)
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (enabled && (scrollByAction == null || scrollByOffsetAction == null)) {
            setScrollSemanticsActions()
        }

        scrollByAction?.let {
            scrollBy(action = it)
        }

        scrollByOffsetAction?.let {
            scrollByOffset(action = it)
        }
    }

    private fun setScrollSemanticsActions() {
        scrollByAction = { x, y ->
            coroutineScope.launch {
                scrollingLogic.semanticsScrollBy(Offset(x, y))
            }
            true
        }

        scrollByOffsetAction = { offset -> scrollingLogic.semanticsScrollBy(offset) }
    }

    private fun clearScrollSemanticsActions() {
        scrollByAction = null
        scrollByOffsetAction = null
    }

    /**
     * Mouse wheel
     */
    private fun processMouseWheelEvent(event: PointerEvent, size: IntSize) {
        if (event.changes.fastAll { !it.isConsumed }) {
            with(scrollConfig!!) {
                val scrollAmount = requireDensity().calculateMouseWheelScroll(event, size)
                // A coroutine is launched for every individual scroll event in the
                // larger scroll gesture. If we see degradation in the future (that is,
                // a fast scroll gesture on a slow device causes UI jank [not seen up to
                // this point), we can switch to a more efficient solution where we
                // lazily launch one coroutine (with the first event) and use a Channel
                // to communicate the scroll amount to the UI thread.
                coroutineScope.launch {
                    scrollingLogic.scroll(scrollPriority = MutatePriority.UserInput) {
                        val directionalDelta =
                            when (orientation) {
                                null -> scrollAmount
                                Orientation.Horizontal -> scrollAmount.copy(y = 0f)
                                Orientation.Vertical -> scrollAmount.copy(x = 0f)
                            }
                        scrollBy(
                            offset = directionalDelta,
                            source = NestedScrollSource.UserInput
                        )
                    }
                }
                event.changes.fastForEach { it.consume() }
            }
        }
    }
}

/**
 * Contains the default values used by [scrollable]
 */
public object Scrollable2DDefaults {

    /**
     * Create and remember default [FlingBehavior] that will represent natural fling curve.
     */
    @Composable
    public fun flingBehavior(): Fling2DBehavior {
        val flingSpec = rememberSplineBasedDecay<Offset>()
        return remember(flingSpec) {
            DefaultFling2DBehavior(flingSpec)
        }
    }

    /**
     * Create and remember default [OverscrollEffect] that will be used for showing over scroll
     * effects.
     */
    @Composable
    @ExperimentalFoundationApi
    public fun overscrollEffect(): OverscrollEffect? {
        return rememberOverscrollEffect()
    }

    /**
     * Used to determine the value of `reverseDirection` parameter of [Modifier.scrollable]
     * in scrollable layouts.
     *
     * @param layoutDirection current layout direction (e.g. from [LocalLayoutDirection])
     * @param orientation orientation of scroll
     * @param reverseScrolling whether scrolling direction should be reversed
     *
     * @return `true` if scroll direction should be reversed, `false` otherwise.
     */
    public fun reverseDirection(
        layoutDirection: LayoutDirection,
        orientation: Orientation,
        reverseScrolling: Boolean,
    ): Boolean {
        // A finger moves with the content, not with the viewport. Therefore,
        // always reverse once to have "natural" gesture that goes reversed to layout
        var reverseDirection = !reverseScrolling
        // But if rtl and horizontal, things move the other way around
        val isRtl = layoutDirection == LayoutDirection.Rtl
        if (isRtl && orientation != Orientation.Vertical) {
            reverseDirection = !reverseDirection
        }
        return reverseDirection
    }
}

internal interface ScrollConfig {
    fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset
}

private val CanDragCalculation: (PointerInputChange) -> Boolean =
    { change -> change.type != PointerType.Mouse }

/**
 * Holds all scrolling related logic: controls nested scrolling, flinging, overscroll and delta
 * dispatching.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class ScrollingLogic(
    private var scrollableState: Scrollable2DState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: Fling2DBehavior,
    private var reverseDirection: Boolean,
    private var nestedScrollDispatcher: NestedScrollDispatcher,
) {
    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

    fun Velocity.reverseIfNeeded(): Velocity = if (reverseDirection) this * -1f else this

    private var latestScrollSource = NestedScrollSource.UserInput
    private var outerStateScope = NoOpScrollScope

    private val nestedScrollScope = object : NestedScrollScope {
        override fun scrollBy(offset: Offset, source: NestedScrollSource): Offset {
            return with(outerStateScope) {
                performScroll(offset, source)
            }
        }

        override fun scrollByWithOverscroll(offset: Offset, source: NestedScrollSource): Offset {
            latestScrollSource = source
            val overscroll = overscrollEffect
            return if (overscroll != null && shouldDispatchOverscroll) {
                overscroll.applyToScroll(offset, latestScrollSource, performScrollForOverscroll)
            } else {
                with(outerStateScope) {
                    performScroll(offset, source)
                }
            }
        }
    }

    private val performScrollForOverscroll: (Offset) -> Offset = { delta ->
        with(outerStateScope) {
            performScroll(delta, latestScrollSource)
        }
    }

    private fun Scroll2DScope.performScroll(delta: Offset, source: NestedScrollSource): Offset {
        val consumedByPreScroll =
            nestedScrollDispatcher.dispatchPreScroll(delta, source)

        val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

        val deltaForSelfScroll =
            scrollAvailableAfterPreScroll.reverseIfNeeded()

        // Consume on a single axis.
        val consumedBySelfScroll =
            scrollBy(deltaForSelfScroll).reverseIfNeeded()

        val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
        val consumedByPostScroll = nestedScrollDispatcher.dispatchPostScroll(
            consumedBySelfScroll,
            deltaAvailableAfterScroll,
            source
        )

        return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
    }

    private val shouldDispatchOverscroll
        get() = scrollableState.canScrollTop || scrollableState.canScrollBottom ||
            scrollableState.canScrollRight || scrollableState.canScrollLeft

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            scrollableState.dispatchRawDelta(scroll.reverseIfNeeded())
                .reverseIfNeeded()
        }
    }

    suspend fun onDragStopped(availableVelocity: Velocity) {
        val performFling: suspend (Velocity) -> Velocity = { velocity ->
            val preConsumedByParent = nestedScrollDispatcher
                .dispatchPreFling(velocity)
            val available = velocity - preConsumedByParent

            val velocityLeft = doFlingAnimation(available)

            val consumedPost =
                nestedScrollDispatcher.dispatchPostFling(
                    (available - velocityLeft),
                    velocityLeft
                )
            val totalLeft = velocityLeft - consumedPost
            velocity - totalLeft
        }

        val overscroll = overscrollEffect
        if (overscroll != null && shouldDispatchOverscroll) {
            overscroll.applyToFling(availableVelocity, performFling)
        } else {
            performFling(availableVelocity)
        }
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available

        scroll(scrollPriority = MutatePriority.Default) {
            val nestedScrollScope = this
            val reverseScope = object : Scroll2DScope {
                override fun scrollBy(pixels: Offset): Offset {
                    return nestedScrollScope.scrollByWithOverscroll(
                        offset = pixels.reverseIfNeeded(),
                        source = SideEffect
                    ).reverseIfNeeded()
                }
            }
            with(reverseScope) {
                with(flingBehavior) {
                    result = performFling(available.reverseIfNeeded()).reverseIfNeeded()
                }
            }
        }
        return result
    }

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.isScrollInProgress ||
            overscrollEffect?.isInProgress == true
    }

    /**
     * Opens a scrolling session with nested scrolling and overscroll support.
     */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend NestedScrollScope.() -> Unit,
    ) {
        scrollableState.scroll(scrollPriority) {
            outerStateScope = this
            block.invoke(nestedScrollScope)
        }
    }

    /**
     * @return true if the pointer input should be reset
     */
    fun update(
        scrollableState: Scrollable2DState,
        overscrollEffect: OverscrollEffect?,
        reverseDirection: Boolean,
        flingBehavior: Fling2DBehavior,
        nestedScrollDispatcher: NestedScrollDispatcher,
    ): Boolean {
        var resetPointerInputHandling = false
        if (this.scrollableState != scrollableState) {
            this.scrollableState = scrollableState
            resetPointerInputHandling = true
        }
        this.overscrollEffect = overscrollEffect
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }
        this.flingBehavior = flingBehavior
        this.nestedScrollDispatcher = nestedScrollDispatcher
        return resetPointerInputHandling
    }
}

private val NoOpScrollScope: Scroll2DScope = object : Scroll2DScope {
    override fun scrollBy(pixels: Offset): Offset = pixels
}

private class ScrollableNestedScrollConnection(
    val scrollingLogic: ScrollingLogic,
    var enabled: Boolean,
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = if (enabled) {
        scrollingLogic.performRawScroll(available)
    } else {
        Offset.Zero
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        return if (enabled) {
            val velocityLeft = scrollingLogic.doFlingAnimation(available)
            available - velocityLeft
        } else {
            Velocity.Zero
        }
    }
}

internal class DefaultFling2DBehavior(
    var flingDecay: DecayAnimationSpec<Offset>,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,
) : Fling2DBehavior {

    // For Testing
    var lastAnimationCycleCount = 0

    override suspend fun Scroll2DScope.performFling(initialVelocity: Velocity): Velocity {
        lastAnimationCycleCount = 0
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return withContext(motionDurationScale) {
            if (abs(initialVelocity.x) > 1f || abs(initialVelocity.y) > 1f) {
                var velocityLeft = Offset(initialVelocity.x, initialVelocity.y)
                var lastValue = Offset.Zero
                var hasStarted = false
                val animationState = AnimationState(
                    typeConverter = Offset.VectorConverter,
                    initialValue = Offset.Zero,
                    initialVelocityVector = AnimationVector2D(initialVelocity.x, initialVelocity.y)
                )
                try {
                    animationState.animateDecay(flingDecay) {
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity
                        // avoid rounding errors and stop if anything is unconsumed on both axes
                        val unconsumedX = abs(delta.x) <= 0.5f || abs(delta.x - consumed.x) > 0.5f
                        val unconsumedY = abs(delta.y) <= 0.5f || abs(delta.y - consumed.y) > 0.5f
                        if (hasStarted && unconsumedX && unconsumedY) {
                            this.cancelAnimation()
                        }
                        lastAnimationCycleCount++
                        hasStarted = true
                    }
                } catch (_: CancellationException) {
                    velocityLeft = animationState.velocity
                }
                Velocity(velocityLeft.x, velocityLeft.y)
            } else {
                initialVelocity
            }
        }
    }
}

private const val DefaultScrollMotionDurationScaleFactor = 1f
internal val DefaultScrollMotionDurationScale = object : MotionDurationScale {
    override val scaleFactor: Float
        get() = DefaultScrollMotionDurationScaleFactor
}

/**
 * (b/311181532): This could not be flattened so we moved it to TraversableNode, but ideally
 * ScrollabeNode should be the one to be travesable.
 */
internal class ScrollableContainerNode(enabled: Boolean) :
    Modifier.Node(),
    TraversableNode {
    override val traverseKey: Any = TraverseKey

    var enabled: Boolean = enabled
        private set

    companion object TraverseKey

    fun update(enabled: Boolean) {
        this.enabled = enabled
    }
}

internal val UnityDensity = object : Density {
    override val density: Float
        get() = 1f
    override val fontScale: Float
        get() = 1f
}

/**
 * A scroll scope for nested scrolling and overscroll support.
 */
internal interface NestedScrollScope {
    fun scrollBy(
        offset: Offset,
        source: NestedScrollSource,
    ): Offset

    fun scrollByWithOverscroll(
        offset: Offset,
        source: NestedScrollSource,
    ): Offset
}

/**
 * Scroll deltas originating from the semantics system. Should be dispatched as an animation
 * driven event.
 */
private suspend fun ScrollingLogic.semanticsScrollBy(offset: Offset): Offset {
    var previousValue = Offset.Zero
    scroll(scrollPriority = MutatePriority.Default) {
        animate(Offset.VectorConverter, Offset.Zero, offset) { currentValue, _ ->
            val delta = currentValue - previousValue
            val consumed =
                scrollBy(
                    offset = delta.reverseIfNeeded(),
                    source = NestedScrollSource.UserInput
                ).reverseIfNeeded()
            previousValue += consumed
        }
    }
    return previousValue
}
