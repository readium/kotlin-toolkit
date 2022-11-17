package org.readium.navigator.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import org.readium.navigator.internal.lazy.LazyItemScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import org.readium.navigator.internal.gestures.*
import org.readium.navigator.internal.gestures.scrollable
import org.readium.navigator.internal.gestures.scrolling
import org.readium.navigator.internal.gestures.tappable
import org.readium.navigator.internal.gestures.zoomable
import org.readium.navigator.internal.lazy.*
import org.readium.navigator.internal.util.ConsumeFlingNestedScrollConnection
import org.readium.navigator.internal.util.FitBox

@Composable
fun LazyViewer(
    modifier: Modifier,
    state: LazyViewerState,
    userScrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit
) {
    when {
        state.layout.vertical && state.layout.snap -> {
            LazyViewerVerticalSnap(
                modifier = modifier,
                state = state,
                userScrollable = userScrollable,
                contentPadding = contentPadding,
                content = content
            )
        }

        state.layout.vertical && !state.layout.snap -> {
            LazyViewerVerticalScroll(
                modifier = modifier,
                state = state,
                userScrollable = userScrollable,
                contentPadding = contentPadding,
                content = content
            )
        }

        !state.layout.vertical && state.layout.snap -> {
            LazyViewerHorizontalSnap(
                modifier = modifier,
                state = state,
                userScrollable = userScrollable,
                contentPadding = contentPadding,
                content = content
            )
        }

        !state.layout.vertical && !state.layout.snap -> {
            LazyViewerHorizontalScroll(
                modifier = modifier,
                state = state,
                userScrollable = userScrollable,
                contentPadding = contentPadding,
                content = content
            )
        }

    }
}

@Composable
private fun LazyColumn(
    modifier: Modifier,
    lazyListState: LazyViewerState,
    horizontalScrollState: ScrollState,
    userScrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    items: List<LazyItemScope.() -> Unit>
) {
    val flingBehavior = rememberSnapFlingBehavior(state.lazyListState)

    LazyColumn(
        modifier = modifier
            .scrollable(
                horizontalState = state.crossAxisScrollState,
                verticalState = state.lazyListState,
                reverseDirection = state.layout.reverse,
                interactionSource = state.lazyListState.internalInteractionSource,
                flingBehavior = flingBehavior,
                enabled = userScrollable
            )
            .tappable(
                enabled = onTap != null || onDoubleTap != null,
                onTap = onTap,
                onDoubleTap = onDoubleTap
            ),
        state = state.lazyListState,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = Alignment.CenterHorizontally,
        reverseLayout = false,
        userScrollEnabled = false,
    ) {

        // We only consume nested flings in the main-axis, allowing cross-axis flings to propagate
        // as normal
        val consumeFlingNestedScrollConnection = ConsumeFlingNestedScrollConnection(
            consumeHorizontal = false,
            consumeVertical = true,
        )

        items(items) { item ->
            Box(
                Modifier
                    .nestedScroll(connection = consumeFlingNestedScrollConnection)
                    .fillParentMaxSize(),
                Alignment.Center
            ) {
                item()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyViewerVerticalSnap(
    modifier: Modifier,
    state: LazyViewerState,
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null,
    userScrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    items: List<LazyItemScope.() -> Unit>
) {
    val flingBehavior = rememberSnapFlingBehavior(state.lazyListState)

    LazyColumn(
        modifier = modifier
            .scrollable(
                horizontalState = state.crossAxisScrollState,
                verticalState = state.lazyListState,
                reverseDirection = state.layout.reverse,
                interactionSource = state.lazyListState.internalInteractionSource,
                flingBehavior = flingBehavior,
                enabled = userScrollable
            )
            .tappable(
                enabled = onTap != null || onDoubleTap != null,
                onTap = onTap,
                onDoubleTap = onDoubleTap
            ),
        state = state.lazyListState,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = Alignment.CenterHorizontally,
        reverseLayout = false,
        userScrollEnabled = false,
    ) {

        // We only consume nested flings in the main-axis, allowing cross-axis flings to propagate
        // as normal
        val consumeFlingNestedScrollConnection = ConsumeFlingNestedScrollConnection(
            consumeHorizontal = false,
            consumeVertical = true,
        )

        items(items) { item ->
            Box(
                Modifier
                    .nestedScroll(connection = consumeFlingNestedScrollConnection)
                    .fillParentMaxSize(),
                Alignment.Center
            ) {
               item()
            }
        }
    }
}

@Composable
private fun LazyViewerVerticalScroll(
    modifier: Modifier,
    state: LazyViewerState,
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null,
    userScrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    items: List<LazyItemScope.() -> Unit>
) {
    val flingBehavior = ScrollableDefaults.flingBehavior()

    LazyColumn(
        modifier = modifier
            .scrollable(
                horizontalState = state.crossAxisScrollState,
                verticalState = state.lazyListState,
                reverseDirection = state.layout.reverse,
                interactionSource = state.lazyListState.internalInteractionSource,
                flingBehavior = flingBehavior,
                enabled = userScrollable
            )
            .scrolling(
                state = state.crossAxisScrollState,
                isVertical = true,
                reverseScrolling = false,
                enabled = userScrollable
            )
            .zoomable(state.zoomState)
            .tappable(
                enabled = onTap != null || onDoubleTap != null,
                onTap = onTap,
                onDoubleTap = onDoubleTap
            ),
        state = state.lazyListState,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = Alignment.CenterHorizontally,
        reverseLayout = false,
        userScrollEnabled = false,
    ) {

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyViewerHorizontalSnap(
    modifier: Modifier,
    state: LazyViewerState,
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null,
    userScrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    items: List<LazyItemScope.() -> Unit>
) {
    val flingBehavior = rememberSnapFlingBehavior(state.lazyListState)

    LazyRow(
        modifier = modifier
            .scrollable(
                horizontalState = state.lazyListState,
                verticalState = state.crossAxisScrollState,
                reverseDirection = state.layout.reverse,
                interactionSource = state.lazyListState.internalInteractionSource,
                flingBehavior = flingBehavior,
                enabled = userScrollable,
            )
            .tappable(
                enabled = onTap != null || onDoubleTap != null,
                onTap = onTap,
                onDoubleTap = onDoubleTap
            ),
        state = state.lazyListState,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        verticalAlignment = Alignment.CenterVertically,
        reverseLayout = state.layout.reverse,
        userScrollEnabled = false,
    ) {
        // We only consume nested flings in the main-axis, allowing cross-axis flings to propagate
        // as normal
        val consumeFlingNestedScrollConnection = ConsumeFlingNestedScrollConnection(
            consumeHorizontal = true,
            consumeVertical = false,
        )

        items(items) { item ->
            Box(
                Modifier
                    .nestedScroll(connection = consumeFlingNestedScrollConnection)
                    .fillParentMaxSize(),
                Alignment.Center
            ) {
                val scaleState = remember { mutableStateOf(1f) }
                item.content(scaleState)
            }
        }
    }
}

@Composable
private fun LazyViewerHorizontalScroll(
    modifier: Modifier,
    state: LazyViewerState,
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null,
    userScrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    items: List<LazyItemScope.() -> Unit>
) {
    // The node with a [scrolling] modifier has unbounded cross-axis size. We need the parent constraints
    // to fit the content into the parent though.

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier,
        propagateMinConstraints = true
    ) {

        val flingBehavior = ScrollableDefaults.flingBehavior()

        val reverseDirection = ScrollableDefaults.reverseDirection(
            LocalLayoutDirection.current,
            Orientation.Horizontal,
            state.layout.reverse
        ),

        LazyRow(
            modifier = modifier
                .scrollable(
                    horizontalState = state.lazyListState,
                    verticalState = state.crossAxisScrollState,
                    reverseDirection = reverseDirection,
                    interactionSource = state.lazyListState.internalInteractionSource,
                    flingBehavior = flingBehavior,
                    enabled = userScrollable,
                )
                .scrolling(
                    state = state.crossAxisScrollState,
                    isVertical = true,
                    reverseScrolling = reverseDirection,
                    enabled = userScrollable
                )
                .zoomable(state.zoomState)
                .tappable(
                    enabled = onTap != null || onDoubleTap != null,
                    onTap = onTap,
                    onDoubleTap = onDoubleTap
                ),
            state = state.lazyListState,
            contentPadding = contentPadding,
            flingBehavior = flingBehavior,
            verticalAlignment = Alignment.CenterVertically,
            reverseLayout = state.layout.reverse,
            userScrollEnabled = false,
        ) {
            val parentSize =
                Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

            items(count = itemCount) { index ->
                FitBox(
                    parentSize = parentSize,
                    contentScale = ContentScale.FillHeight,
                    scaleSetting = state.zoomState.scaleState.value,
                    itemSize = itemSize(index),
                    content = { itemContent(index) }
                )
            }
        }
    }
}

