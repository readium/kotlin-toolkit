package org.readium.r2.navigator3.viewer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import org.readium.r2.navigator3.gestures.ScrollState
import org.readium.r2.navigator3.gestures.ZoomableState
import org.readium.r2.navigator3.lazy.LazyListState
import timber.log.Timber

internal class LazyScrollerState(
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
        val Saver: Saver<LazyScrollerState, *> = listSaver(
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
                LazyScrollerState(
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
internal fun rememberLazyScrollerState(
    isLazyVertical: Boolean,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    initialOppositeDirectionScrollOffset: Int = 0,
    initialScale: Float = 1f,
): LazyScrollerState {
    return rememberSaveable(saver = LazyScrollerState.Saver) {
        LazyScrollerState(
            if (isLazyVertical) Orientation.Vertical else Orientation.Horizontal,
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
            initialOppositeDirectionScrollOffset,
            initialScale
        )
    }
}
