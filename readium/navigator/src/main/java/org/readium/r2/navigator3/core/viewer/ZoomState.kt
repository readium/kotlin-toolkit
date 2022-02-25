package org.readium.r2.navigator3.core.viewer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import org.readium.r2.navigator3.core.gestures.ZoomableState
import org.readium.r2.navigator3.core.lazy.LazyListState
import timber.log.Timber

internal class ZoomState(
    private val mainAxisOrientation: Orientation,
    private val mainAxisScrollState: LazyListState,
    private val crossAxisScrollState: ScrollableState,
    override val scaleState: MutableState<Float>,
) : ZoomableState {

    override fun onScaleChanged(zoomChange: Float, centroid: Offset) {
        val mainAxisCentroid =
            if (mainAxisOrientation == Orientation.Horizontal) centroid.x else centroid.y

        val firstVisibleItemScrollOffset = mainAxisScrollState.firstVisibleItemScrollOffsetNonObservable
        val scrollToBeConsumed = mainAxisScrollState.scrollToBeConsumed

        //FIXME: there is still a bug between resources
        val mainAxisDelta =
            firstVisibleItemScrollOffset * zoomChange - firstVisibleItemScrollOffset +
                    scrollToBeConsumed * zoomChange - scrollToBeConsumed +
                    mainAxisCentroid * zoomChange - mainAxisCentroid

        Timber.d("lazyAxisDelta $mainAxisDelta")
        mainAxisScrollState.dispatchRawDelta(mainAxisDelta)

        val crossAxisCentroid =
            if (mainAxisOrientation == Orientation.Horizontal) centroid.y else centroid.x

        val otherAxisDelta = crossAxisCentroid * zoomChange - crossAxisCentroid

        crossAxisScrollState.dispatchRawDelta(otherAxisDelta)
    }
}

@Composable
internal fun rememberZoomState(
    mainAxisOrientation: Orientation,
    mainAxisScrollState: LazyListState,
    crossAxisScrollState: ScrollableState,
    initialScale: Float = 0f
) = remember {
    ZoomState(
        mainAxisOrientation,
        mainAxisScrollState,
        crossAxisScrollState,
        mutableStateOf(initialScale)
    )
}
