package org.readium.navigator.internal.gestures

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import org.readium.navigator.internal.lazy.LazyListState
import timber.log.Timber

class ZoomState(
    private val lazyListState: LazyListState,
    private val crossAxisScrollState: ScrollableState,
    var orientation: Orientation,
    override val scaleState: MutableState<Float>,
) : ZoomableState {

    override fun onScaleChanged(zoomChange: Float, centroid: Offset) {
        val mainAxisCentroid =
            if (orientation == Orientation.Horizontal) centroid.x else centroid.y

        val firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset
        val scrollToBeConsumed = lazyListState.scrollToBeConsumed

        //FIXME: there is still a bug between resources
        val mainAxisDelta =
            firstVisibleItemScrollOffset * zoomChange - firstVisibleItemScrollOffset +
                    scrollToBeConsumed * zoomChange - scrollToBeConsumed +
                    mainAxisCentroid * zoomChange - mainAxisCentroid

        Timber.d("lazyAxisDelta $mainAxisDelta")
        lazyListState.dispatchRawDelta(mainAxisDelta)

        val crossAxisCentroid =
            if (orientation == Orientation.Horizontal) centroid.y else centroid.x

        val otherAxisDelta = crossAxisCentroid * zoomChange - crossAxisCentroid

        crossAxisScrollState.dispatchRawDelta(otherAxisDelta)
    }
}
