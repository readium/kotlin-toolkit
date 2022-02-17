package org.readium.r2.navigator3.core.scroller

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator3.core.gestures.scrollable
import org.readium.r2.navigator3.core.gestures.scrolling
import org.readium.r2.navigator3.core.gestures.zoomable
import org.readium.r2.navigator3.core.lazy.LazyItemScope
import org.readium.r2.navigator3.core.lazy.LazyList
import org.readium.r2.navigator3.core.lazy.LazyListScope
import org.readium.r2.navigator3.core.lazy.rememberStateOfItemsProvider
import kotlin.math.ceil

@Composable
internal fun LazyScroller(
    modifier: Modifier = Modifier,
    isVertical: Boolean,
    state: LazyScrollerState = rememberLazyScrollerState(isVertical),
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
                    horizontalState = if (isVertical) state.otherScrollState else state.lazyListState,
                    verticalState = if (isVertical) state.lazyListState else state.otherScrollState,
                    reverseDirection = reverseScrollDirection,
                    interactionSource = state.lazyListState.internalInteractionSource,
                    flingBehavior = flingBehavior
                )
                .scrolling(
                    state = state.otherScrollState,
                    isVertical = !isVertical,
                    reverseScrolling = reverseScrollDirection
                )
                .zoomable(state),
            stateOfItemsProvider = rememberStateOfItemsProvider {
                scrollerContent(
                    isVertical = isVertical,
                    parentConstraints = constraints,
                    scaleSetting = state.scaleState.value,
                    count = count,
                    itemSize = itemSize,
                    itemContent = itemContent
                )
            },
            state = state.lazyListState,
            contentPadding = contentPadding,
            flingBehavior = flingBehavior,
            horizontalAlignment = horizontalAlignment,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            verticalArrangement = verticalArrangement,
            isVertical = isVertical,
            reverseLayout = reverseLayout
        )
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
        ItemBox(
            parentSize= parentSize,
            contentScale = contentScale,
            scaleSetting = scaleSetting,
            itemSize = itemSize(index),
            content = { itemContent(index) }
        )
    }
}

@Composable
private fun ItemBox(
    parentSize: Size,
    contentScale: ContentScale,
    scaleSetting: Float,
    itemSize: Size,
    content: @Composable BoxScope.() -> Unit
) {
    val initialItemScale = remember {
      contentScale.computeScaleFactor(itemSize, parentSize).scaleX
    }

    val itemScale = initialItemScale * scaleSetting

    val width =  with(LocalDensity.current) {
        ceil(itemSize.width * itemScale).toDp()
    }

    val height =  with(LocalDensity.current) {
        ceil(itemSize.height * itemScale).toDp()
    }

    Box(
        modifier = Modifier.requiredSize(width, height),
        content = content
    )
}
