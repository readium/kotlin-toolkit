package org.readium.r2.navigator3.core.scroller

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator3.core.gestures.scrollable
import org.readium.r2.navigator3.core.gestures.scrolling
import org.readium.r2.navigator3.core.gestures.zoomable
import org.readium.r2.navigator3.core.lazy.LazyItemScope
import org.readium.r2.navigator3.core.lazy.LazyList
import org.readium.r2.navigator3.core.lazy.LazyListScope
import org.readium.r2.navigator3.core.lazy.rememberStateOfItemsProvider
import org.readium.r2.navigator3.core.scroller.LazyScrollerState
import org.readium.r2.navigator3.core.scroller.rememberLazyScrollerState

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
    itemContent: @Composable LazyItemScope.(index: Int) -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reverseLayout =  if (isVertical || !isRtl) reverseDirection else !reverseDirection
    // reverse scroll by default, to have "natural" gesture that goes reversed to layout
    // if rtl and horizontal, do not reverse to make it right-to-left
    val reverseScrollDirection = !reverseLayout

    val flingBehavior = ScrollableDefaults.flingBehavior()

    @Suppress("NAME_SHADOWING")
    val modifier = modifier.scrollable(
        horizontalState = if (isVertical) state.otherScrollState else state.lazyListState,
        verticalState = if (isVertical) state.lazyListState else state.otherScrollState,
        reverseDirection = reverseScrollDirection,
        interactionSource = state.lazyListState.internalInteractionSource,
        flingBehavior = flingBehavior
    ).scrolling(
        state = state.otherScrollState,
        isVertical = !isVertical,
        reverseScrolling = reverseScrollDirection
    ).zoomable(state)

    val content: (LazyListScope).() -> Unit = {
        items(count = count) { index ->
            itemContent(index)
        }
    }

    LazyList(
        modifier = modifier,
        stateOfItemsProvider = rememberStateOfItemsProvider(content),
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
