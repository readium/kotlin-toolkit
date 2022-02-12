package org.readium.r2.navigator3.viewer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.SnapOffsets
import org.readium.r2.navigator3.gestures.ScrollState
import org.readium.r2.navigator3.gestures.ZoomableState
import org.readium.r2.navigator3.gestures.scrollable
import org.readium.r2.navigator3.lazy.LazyList
import org.readium.r2.navigator3.lazy.LazyListScope
import org.readium.r2.navigator3.lazy.LazyListState
import org.readium.r2.navigator3.lazy.rememberStateOfItemsProvider
import timber.log.Timber

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
    content: LazyListScope.() -> Unit
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

    @Suppress("NAME_SHADOWING")
    val modifier = modifier.scrollable(
        horizontalState = if (isVertical) state.otherScrollState else state.lazyListState,
        verticalState = if (isVertical) state.lazyListState else state.otherScrollState,
        reverseDirection = reverseScrollDirection,
        interactionSource = state.lazyListState.internalInteractionSource,
        flingBehavior = flingBehavior
    )

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


internal class LazyPagerState(
    private val lazyOrientation: Orientation,
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    oppositeDirectionScrollOffset: Int = 0,
    scale: Float = 1f,
) : ZoomableState {
    val lazyListState: LazyListState =
        LazyListState(firstVisibleItemIndex, firstVisibleItemScrollOffset)

    val otherScrollState: ScrollState =
        ScrollState(oppositeDirectionScrollOffset)

    override var scaleState: MutableState<Float> =
        mutableStateOf(scale)

    override fun onScaleChanged(zoomChange: Float, centroid: Offset) {
        val lazyAxisCentroid =
            if (lazyOrientation == Orientation.Horizontal) centroid.x else centroid.y

        val firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffsetNonObservable
        val scrollToBeConsumed = lazyListState.scrollToBeConsumed

        //FIXME: there is still a bug between resources
        val lazyAxisDelta =
            firstVisibleItemScrollOffset * zoomChange - firstVisibleItemScrollOffset +
                    scrollToBeConsumed * zoomChange - scrollToBeConsumed +
                    lazyAxisCentroid * zoomChange - lazyAxisCentroid

        Timber.d("lazyAxisDelta $lazyAxisDelta")
        lazyListState.dispatchRawDelta(lazyAxisDelta)

        val otherAxisCentroid =
            if (lazyOrientation == Orientation.Horizontal) centroid.y else centroid.x

        val otherAxisDelta = otherAxisCentroid * zoomChange - otherAxisCentroid

        otherScrollState.dispatchRawDelta(otherAxisDelta)
    }

    companion object {
        val Saver: Saver<LazyPagerState, *> = listSaver(
            save = {
                listOf(
                    it.lazyOrientation == Orientation.Vertical,
                    it.lazyListState.firstVisibleItemIndex,
                    it.lazyListState.firstVisibleItemScrollOffset,
                    it.otherScrollState.value,
                    it.scaleState.value
                )
            },
            restore = {
                LazyPagerState(
                    if (it[0] as Boolean) Orientation.Vertical else Orientation.Horizontal,
                    firstVisibleItemIndex = it[1] as Int,
                    firstVisibleItemScrollOffset = it[2] as Int,
                    oppositeDirectionScrollOffset = it[3] as Int,
                    scale = it[4] as Float
                )
            }
        )
    }
}

@Composable
internal fun rememberLazyPagerState(
    isLazyVertical: Boolean,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialOppositeDirectionScrollOffset: Int = 0,
    initialScale: Float = 1f,
): LazyPagerState {
    return rememberSaveable(saver = LazyPagerState.Saver) {
        LazyPagerState(
            if (isLazyVertical) Orientation.Vertical else Orientation.Horizontal,
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
            initialOppositeDirectionScrollOffset,
            initialScale
        )
    }
}
