package org.readium.navigator.core

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.*
import org.readium.navigator.internal.gestures.ScrollState
import org.readium.navigator.internal.lazy.LazyListItemInfo
import org.readium.navigator.internal.lazy.LazyListState
import kotlin.math.abs

data class LazyViewerLayout(
    val snap: Boolean,
    val vertical: Boolean,
    val reverse: Boolean,
)

class LazyViewerState(
    initialLayout: LazyViewerLayout,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialCrossAxisScrollOffset: Int = 0,
    initialScale: Float = 1f,
) {
    var layout: LazyViewerLayout by mutableStateOf(initialLayout)

    val lazyListState: LazyListState get() =
        state.lazyListState

    val crossAxisScrollState: ScrollState get() =
        state.crossAxisScrollState

    val zoomState: ZoomState
        get() =
        state.zoomState

    val visibleItemInfo: List<LazyListItemInfo> get() =
        state.visibleItemInfo

    val totalItemsCount: Int get() =
        state.totalItemCount

    val firstVisibleItemIndex: Int get() =
        visibleItemInfo.firstOrNull()?.index ?: 0

    suspend fun scrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int = 0
    ) = state.lazyListState.scrollToItem(index, scrollOffset)

    suspend fun animateScrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int = 0
    ) = state.lazyListState.animateScrollToItem(index, scrollOffset)

    private data class InternalState(
        val lazyListState: LazyListState,
        val crossAxisScrollState: ScrollState,
        val zoomState: ZoomState,
    ) {

        val firstVisibleItemIndex: Int get() =
            lazyListState.firstVisibleItemIndex

        val firstVisibleItemScrollOffset: Int get() =
            lazyListState.firstVisibleItemScrollOffset

        val crossAxisScrollOffset: Int get() =
            crossAxisScrollState.value

        val scale: Float get() =
            zoomState.scaleState.value

        val visibleItemInfo: List<LazyListItemInfo> get() =
            lazyListState.layoutInfo.visibleItemsInfo.run {
                if (layout.value.snap)
                    this.filter { abs(it.offset) != it.size }
                else
                    this
            }

        val totalItemCount: Int get() =
            lazyListState.layoutInfo.totalItemsCount
    }

    private val state by createState(
        initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset,
        initialCrossAxisScrollOffset,
        initialScale,
    )

    private fun createState(
        initialFirstVisibleItemIndex: Int,
        initialFirstVisibleItemScrollOffset: Int,
        initialCrossAxisScrollOffset: Int,
        initialScale: Float,
    ): State<InternalState> = derivedStateOf{
        val lazyListState = LazyListState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset
        )

        val crossAxisScrollState = ScrollState(
            initialCrossAxisScrollOffset
        )

        val zoomState = ZoomState(
                if (layout.vertical) Orientation.Vertical else Orientation.Horizontal,
                lazyListState,
                crossAxisScrollState,
                mutableStateOf(initialScale)
            )

        InternalState(lazyListState, crossAxisScrollState, zoomState)
    }
}

@Composable
fun rememberLazyViewerState(
    initialLayout: LazyViewerLayout,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialCrossAxisScrollOffset: Int = 0,
    initialScale: Float = 1f,
): LazyViewerState = remember {
    LazyViewerState(
        initialLayout,
        initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset,
        initialCrossAxisScrollOffset,
        initialScale
    )
}
