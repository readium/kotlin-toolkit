package org.readium.navigator.image.viewer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.readium.navigator.internal.gestures.ScrollState
import org.readium.navigator.internal.gestures.ZoomState
import org.readium.navigator.internal.gestures.zoomable
import org.readium.navigator.internal.lazy.LazyListState
import org.readium.navigator.internal.util.*
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
@Composable
fun ImageViewer(
    modifier: Modifier,
    state: ImageViewerState
) {
    val layout by state.layout
    val spreads by state.spreads

    when {
        !layout.snap && layout.orientation == Orientation.Vertical ->
            ImageViewerVerticalScroll(
                modifier = modifier,
                lazyListState = state.lazyListState,
                horizontalScrollState = state.crossAxisScrollState,
                zoomState = state.zoomState,
                reverseLayout = layout.reverseLayout,
                contentScale = layout.contentScale,
                items = spreads
            )

        !layout.snap && layout.orientation == Orientation.Horizontal ->
            ImageViewerVerticalScroll(
                modifier = modifier,
                lazyListState = state.lazyListState,
                horizontalScrollState = state.crossAxisScrollState,
                zoomState = state.zoomState,
                reverseLayout = layout.reverseLayout,
                contentScale = layout.contentScale,
                items = spreads
            )
        layout.snap && layout.orientation == Orientation.Vertical ->
            ImageViewerHorizontalPaginated(
                modifier = modifier,
                lazyListState = state.lazyListState,
                reverseLayout = layout.reverseLayout,
                contentScale = layout.contentScale,
                items = spreads
            )
        layout.snap && layout.orientation == Orientation.Horizontal ->
            ImageViewerHorizontalPaginated(
                modifier = modifier,
                lazyListState = state.lazyListState,
                reverseLayout = layout.reverseLayout,
                contentScale = layout.contentScale,
                items = spreads
            )
    }
}

@Composable
fun ImageViewerHorizontalPaginated(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
    contentScale: ContentScale,
    items: List<ImageData>
) {
    EnhancedLazyRow(
        modifier = modifier,
        lazyListState = lazyListState,
        reverseLayout = reverseLayout,
        verticalScrollState = ScrollState(0),
        snap = true,
        horizontalArrangement = if (!reverseLayout) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(items) { index, item ->
            Box(
                modifier = Modifier
                    /*.nestedScroll(
                        connection = ConsumeFlingNestedScrollConnection(
                            consumeHorizontal = false, consumeVertical = true
                        )
                    )*/
                    .fillParentMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                val isVisible = index in visibleItems.indices
                ScrollableImage(
                    resetZoom = false,
                    getImageBitmap = item.content,
                    itemSize = item.size,
                    contentScale = contentScale
                )
            }
        }
    }
}

@Composable
fun ImageViewerVerticalScroll(
    modifier: Modifier,
    lazyListState: LazyListState,
    horizontalScrollState: ScrollState,
    zoomState: ZoomState,
    reverseLayout: Boolean,
    contentScale: ContentScale,
    items: List<ImageData>
) {
    EnhancedLazyColumn(
        modifier = modifier
            .zoomable(zoomState),
        lazyListState = lazyListState,
        reverseLayout = reverseLayout,
        horizontalScrollState = horizontalScrollState,
        snap = false,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        items(items) { item ->
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                propagateMinConstraints = true
            ) {
                FitBox(
                    maxWidth = this@items.maxWidth.value,
                    maxHeight = this@items.maxHeight.value,
                    contentScale = contentScale,
                    scaleSetting = zoomState.scaleState.value,
                    itemSize = item.size,
                ) {
                    ImageOrPlaceholder(item.content)
                }
            }
        }
    }
}
