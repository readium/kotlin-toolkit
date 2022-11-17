package org.readium.navigator.image.viewer

import android.util.Size
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.readium.navigator.internal.gestures.ScrollState
import org.readium.navigator.internal.gestures.ZoomState
import org.readium.navigator.internal.lazy.LazyListState
import org.readium.r2.shared.ExperimentalReadiumApi

class ImageData(
    val size: Size,
    val content: suspend () -> ImageBitmap?
)

data class ImageViewerLayout(
    val contentScale: ContentScale,
    val reverseLayout: Boolean,
    val snap: Boolean,
    val orientation: Orientation
)

@ExperimentalReadiumApi
class ImageViewerState(
    images: List<ImageData>,
    initialLayout: ImageViewerLayout,
    initialImageIndex: Int,
) {
    private var layoutMutable: MutableState<ImageViewerLayout> =
        mutableStateOf(initialLayout)

    internal val spreads: State<List<ImageData>> =
        mutableStateOf(images)

    internal val lazyListState: LazyListState =
        LazyListState(initialImageIndex)

    internal val crossAxisScrollState: ScrollState =
        ScrollState(0)

    internal val zoomState = ZoomState(
        lazyListState,
        crossAxisScrollState,
        layoutMutable.value.orientation,
        mutableStateOf(1.0f)
    )

    val layout: State<ImageViewerLayout> =
        layoutMutable

    fun updateLayout(layout: ImageViewerLayout) {
        zoomState.orientation = layout.orientation
        this.layoutMutable.value = layout
    }
}