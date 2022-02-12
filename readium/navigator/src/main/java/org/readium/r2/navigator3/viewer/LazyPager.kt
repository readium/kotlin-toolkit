package org.readium.r2.navigator3.viewer

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.SnapOffsets
import org.readium.r2.navigator3.gestures.*
import org.readium.r2.navigator3.lazy.LazyItemScope
import org.readium.r2.navigator3.lazy.LazyList
import org.readium.r2.navigator3.lazy.LazyListScope
import org.readium.r2.navigator3.lazy.rememberStateOfItemsProvider

@Composable
@OptIn(ExperimentalSnapperApi::class)
internal fun LazyPager(
    modifier: Modifier = Modifier,
    isVertical: Boolean,
    state: LazyPagerState = rememberLazyPagerState(isVertical),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseDirection: Boolean = false,
    verticalArrangement: Arrangement.Vertical? = null,
    horizontalArrangement: Arrangement.Horizontal? = null,
    verticalAlignment: Alignment.Vertical? = null,
    horizontalAlignment: Alignment.Horizontal? = null,
    count: Int,
    itemContent: @Composable LazyItemScope.(index: Int, scale: Float) -> Unit,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reverseLayout =  if (isVertical || !isRtl) reverseDirection else !reverseDirection
    // reverse scroll by default, to have "natural" gesture that goes reversed to layout
    // if rtl and horizontal, do not reverse to make it right-to-left
    val reverseScrollDirection = !reverseLayout

    val flingBehavior = rememberSnapperFlingBehavior(
        lazyListState = state.lazyListState,
        snapOffsetForItem = SnapOffsets.Start,
        maximumFlingDistance = { it.currentItem?.size?.toFloat() ?: 0f }
    )

    val dummyScrollableState = ScrollableState { 0f }

    @Suppress("NAME_SHADOWING")
    val modifier = modifier.scrollable(
        horizontalState = if (isVertical) dummyScrollableState else state.lazyListState,
        verticalState = if (isVertical) state.lazyListState else dummyScrollableState,
        reverseDirection = reverseScrollDirection,
        interactionSource = state.lazyListState.internalInteractionSource,
        flingBehavior = flingBehavior
    )

    // We only consume nested flings in the main-axis, allowing cross-axis flings to propagate
    // as normal
    val consumeFlingNestedScrollConnection = ConsumeFlingNestedScrollConnection(
        consumeHorizontal = !isVertical,
        consumeVertical = isVertical,
    )

   val content: (LazyListScope).() -> Unit = {
       items(count = count) { index ->

           val pageState = remember { PageZoomState(1f, Offset.Zero)}

           Box(
               Modifier
                   .nestedScroll(connection = consumeFlingNestedScrollConnection)
                   .fillParentMaxSize()
                   .wrapContentSize()
                   .scrollable(
                       horizontalState = pageState.horizontalScrollState,
                       verticalState = pageState.verticalScrollState,
                       reverseDirection = reverseScrollDirection,
                   )
                   .scrolling(
                       state = pageState.verticalScrollState,
                       isVertical = true,
                       reverseScrolling = reverseScrollDirection
                   )
                   .scrolling(
                       state = pageState.horizontalScrollState,
                       isVertical = false,
                       reverseScrolling = reverseScrollDirection
                   )
                   .zoomable(pageState)
           ) {
               itemContent(index, pageState.scaleState.value)
           }
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

private class PageZoomState(
    initialScale: Float,
    initialOffset: Offset
) : ZoomableState {

    val horizontalScrollState: ScrollState = ScrollState(initialOffset.x.toInt())

    val verticalScrollState: ScrollState = ScrollState(initialOffset.y.toInt())

    override var scaleState: MutableState<Float> = mutableStateOf(initialScale)

    override fun onScaleChanged(zoomChange: Float, centroid: Offset) {
        val horizontalDelta = centroid.x * zoomChange - centroid.x
        horizontalScrollState.dispatchRawDelta(horizontalDelta)

        val verticalDelta = centroid.y * zoomChange - centroid.y
        verticalScrollState.dispatchRawDelta(verticalDelta)
    }
}

private class ConsumeFlingNestedScrollConnection(
    private val consumeHorizontal: Boolean,
    private val consumeVertical: Boolean,
) : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = when (source) {
        // We can consume all resting fling scrolls so that they don't propagate up to the
        // Pager
        NestedScrollSource.Fling -> available.consume(consumeHorizontal, consumeVertical)
        else -> Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        // We can consume all post fling velocity on the main-axis
        // so that it doesn't propagate up to the Pager
        return available.consume(consumeHorizontal, consumeVertical)
    }

    private fun Offset.consume(
        consumeHorizontal: Boolean,
        consumeVertical: Boolean,
    ): Offset = Offset(
        x = if (consumeHorizontal) this.x else 0f,
        y = if (consumeVertical) this.y else 0f,
    )

    private fun Velocity.consume(
        consumeHorizontal: Boolean,
        consumeVertical: Boolean,
    ): Velocity = Velocity(
        x = if (consumeHorizontal) this.x else 0f,
        y = if (consumeVertical) this.y else 0f,
    )
}
