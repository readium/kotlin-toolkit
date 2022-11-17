package org.readium.navigator.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableState
import org.readium.navigator.internal.lazy.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import org.readium.navigator.internal.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.readium.navigator.internal.gestures.scrollable
import org.readium.navigator.internal.gestures.tappable
import org.readium.navigator.internal.lazy.LazyListScope
import org.readium.navigator.internal.lazy.LazyList
import org.readium.navigator.internal.util.logConstraints

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyPager(
    modifier: Modifier = Modifier,
    isVertical: Boolean,
    state: LazyViewerState = rememberLazyViewerState(isVertical, isPaginated = true),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseDirection: Boolean = false,
    verticalArrangement: Arrangement.Vertical? = null,
    horizontalArrangement: Arrangement.Horizontal? = null,
    verticalAlignment: Alignment.Vertical? = null,
    horizontalAlignment: Alignment.Horizontal? = null,
    userScrollable: Boolean = true,
    count: Int,
    onTap: ((Offset) -> Unit)?,
    onDoubleTap: ((Offset) -> Unit)?,
    itemContent: @Composable LazyItemScope.(index: Int, scaleState: MutableState<Float>) -> Unit,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reverseLayout =  if (isVertical || !isRtl) reverseDirection else !reverseDirection
    // reverse scroll by default, to have "natural" gesture that goes reversed to layout
    // if rtl and horizontal, do not reverse to make it right-to-left
    val reverseScrollDirection = !reverseLayout

    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = state.lazyListState,
    )

    val dummyScrollableState = ScrollableState { 0f }

    LazyList(
        modifier = modifier
            .scrollable(
                enabled = userScrollable,
                horizontalState = if (isVertical) dummyScrollableState else state.lazyListState,
                verticalState = if (isVertical) state.lazyListState else dummyScrollableState,
                reverseDirection = reverseScrollDirection,
                interactionSource = state.lazyListState.internalInteractionSource,
                flingBehavior = flingBehavior
            )
            .tappable(
                enabled = onTap != null  || onDoubleTap != null,
                onTap = onTap,
                onDoubleTap = onDoubleTap
            ),
        state = state.lazyListState,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = horizontalAlignment,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        verticalArrangement = verticalArrangement,
        isVertical = isVertical,
        reverseLayout = reverseLayout,
        userScrollEnabled = false,
    ) {
        pagerContent(
            isVertical,
            count,
            //state.visibleItemInfo,
            itemContent
        )
    }
}

private fun (LazyListScope).pagerContent(
    isVertical: Boolean,
    count: Int,
    //visibleItems: List<LazyListItemInfo>,
    itemContent: @Composable LazyItemScope.(index: Int, scaleState: MutableState<Float>) -> Unit
) {

    // We only consume nested flings in the main-axis, allowing cross-axis flings to propagate
    // as normal
    val consumeFlingNestedScrollConnection = ConsumeFlingNestedScrollConnection(
        consumeHorizontal = !isVertical,
        consumeVertical = isVertical,
    )

    items(count = count) { index ->
        Box(
            Modifier
                .logConstraints("pagerContentBefore")
                .nestedScroll(connection = consumeFlingNestedScrollConnection)
                .fillParentMaxSize()
                .logConstraints("pagerContentAfter"),
            Alignment.Center
        ) {
            val scaleState = remember { mutableStateOf(1f)}

            //FIXME: this causes a recomposition loop.
            /*if (visibleItems.size == 1 && visibleItems.first().index != index) {
                scaleState.value = 1f
            }*/

            itemContent(index, scaleState)
        }
    }
}
