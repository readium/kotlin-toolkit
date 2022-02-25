package org.readium.r2.navigator3.core.viewer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.readium.r2.navigator3.core.gestures.ScrollState
import org.readium.r2.navigator3.core.lazy.LazyListItemInfo
import org.readium.r2.navigator3.core.lazy.LazyListState
import kotlin.math.abs

internal class LazyViewerState(
    val isVertical: Boolean,
    var isPaginated: Boolean,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialCrossAxisScrollOffset: Int = 0,
    initialScale: Float = 1f,
) {

    val lazyListState =
        LazyListState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset
        )

    val crossAxisScrollState =
        ScrollState(
            initialCrossAxisScrollOffset
        )

    val zoomState =
        ZoomState(
            if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            lazyListState,
            crossAxisScrollState,
            mutableStateOf(initialScale)
        )

    val visibleItemInfo: List<LazyListItemInfo>
        get() = lazyListState.layoutInfo.visibleItemsInfo
            .run {
                if (isPaginated)
                    this.filter { abs(it.offset) != it.size }
                else
                    this
            }

    suspend fun scrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int = 0
    ) = lazyListState.scrollToItem(index, scrollOffset)

    suspend fun animateScrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int = 0
    ) = lazyListState.animateScrollToItem(index, scrollOffset)
}

@Composable
internal fun rememberLazyViewerState(
    isVertical: Boolean,
    isPaginated: Boolean,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialCrossAxisScrollOffset: Int = 0,
    initialScale: Float = 1f,
): LazyViewerState = remember {
    LazyViewerState(
        isVertical,
        isPaginated,
        initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset,
        initialCrossAxisScrollOffset,
        initialScale
    )
}
