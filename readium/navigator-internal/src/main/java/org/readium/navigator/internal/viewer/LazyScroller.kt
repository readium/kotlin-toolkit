package org.readium.navigator.internal.viewer

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.readium.navigator.internal.gestures.scrollable
import org.readium.navigator.internal.gestures.scrolling
import org.readium.navigator.internal.gestures.zoomable
import androidx.compose.foundation.lazy.LazyItemScope
import org.readium.navigator.internal.lazy.LazyList
import org.readium.navigator.internal.lazy.LazyListScope
import org.readium.navigator.internal.util.FitBox

@Composable
internal fun LazyScroller(
    modifier: Modifier = Modifier,
    isVertical: Boolean,
    state: LazyViewerState = rememberLazyViewerState(isVertical, isPaginated = false),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseDirection: Boolean = false,
    verticalArrangement: Arrangement.Vertical? = null,
    horizontalArrangement: Arrangement.Horizontal? = null,
    verticalAlignment: Alignment.Vertical? = null,
    horizontalAlignment: Alignment.Horizontal? = null,
    count: Int,
    itemSize: (Int) -> Size,
    itemContent: @Composable LazyItemScope.(index: Int) -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reverseLayout =  if (isVertical || !isRtl) reverseDirection else !reverseDirection
    // reverse scroll by default, to have "natural" gesture that goes reversed to layout
    // if rtl and horizontal, do not reverse to make it right-to-left
    val reverseScrollDirection = !reverseLayout

    val flingBehavior = ScrollableDefaults.flingBehavior()

    // The node with a [scrolling] modifier has unbounded cross-axis size. We need the parent constraints
    // to fit the content into the parent though.

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier,
        propagateMinConstraints = true
    ) {
        LazyList(
            modifier = Modifier
                .scrollable(
                    horizontalState = if (isVertical) state.crossAxisScrollState else state.lazyListState,
                    verticalState = if (isVertical) state.lazyListState else state.crossAxisScrollState,
                    reverseDirection = reverseScrollDirection,
                    interactionSource = state.lazyListState.internalInteractionSource,
                    flingBehavior = flingBehavior
                )
                .scrolling(
                    state = state.crossAxisScrollState,
                    isVertical = !isVertical,
                    reverseScrolling = reverseScrollDirection
                )
                .zoomable(state.zoomState),
            state = state.lazyListState,
            contentPadding = contentPadding,
            flingBehavior = flingBehavior,
            horizontalAlignment = horizontalAlignment,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            verticalArrangement = verticalArrangement,
            isVertical = isVertical,
            reverseLayout = reverseLayout,
            userScrollEnabled = false
        ) {
            scrollerContent(
                isVertical = isVertical,
                parentConstraints = constraints,
                scaleSetting = state.zoomState.scaleState.value,
                count = count,
                itemSize = itemSize,
                itemContent = itemContent
            )
        }
    }
}

private fun LazyListScope.scrollerContent(
    isVertical: Boolean,
    parentConstraints: Constraints,
    scaleSetting: Float,
    count: Int,
    itemSize: (Int) -> Size,
    itemContent: @Composable LazyItemScope.(index: Int) -> Unit
) {
    val parentSize =
        Size(parentConstraints.maxWidth.toFloat(), parentConstraints.maxHeight.toFloat())

    val contentScale =
        if (isVertical) ContentScale.FillWidth else ContentScale.FillHeight

    items(count = count) { index ->
        FitBox(
            parentSize= parentSize,
            contentScale = contentScale,
            scaleSetting = scaleSetting,
            itemSize = itemSize(index),
            content = { itemContent(index) }
        )
    }
}


