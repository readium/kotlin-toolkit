package org.readium.r2.navigator3.viewer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.readium.r2.navigator3.lazy.LazyListState

internal class LazyPagerState(
    private val lazyOrientation: Orientation,
    currentPage: Int = 0,
    scale: Float = 1f,
) {
    val lazyListState: LazyListState =
        LazyListState(currentPage, 0)
}

@Composable
internal fun rememberLazyPagerState(
    isLazyVertical: Boolean,
    initialPage: Int = 0,
    initialScale: Float = 1f
): LazyPagerState {
    return remember {
        LazyPagerState(
            if (isLazyVertical) Orientation.Vertical else Orientation.Horizontal,
            initialPage,
            initialScale
        )
    }
}
