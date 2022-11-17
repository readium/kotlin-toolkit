package org.readium.navigator.image.viewer

import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.readium.navigator.internal.gestures.ZoomState
import org.readium.navigator.internal.util.FitBox
import org.readium.navigator.internal.util.ZoomableBox
import org.readium.navigator.internal.util.rememberZoomableBoxState

@Composable
fun ScrollableImage(
    resetZoom: Boolean,
    getImageBitmap: suspend () -> ImageBitmap?,
    itemSize: Size,
    contentScale: ContentScale
) {
    val scaleState = remember { mutableStateOf(1.0f) }
    val zoomState = rememberZoomableBoxState(scaleState)

    if (resetZoom) {
        zoomState.scaleState.value = 1.0f
    }

    BoxWithConstraints {
        ZoomableBox(
            modifier = Modifier.fillMaxSize(),
            state = zoomState
        ) {
            FitBox(
                maxWidth = constraints.maxWidth,
                maxHeight = constraints.maxHeight,
                contentScale = contentScale,
                scaleSetting = zoomState.scaleState.value,
                itemSize = itemSize,
                content = { ImageOrPlaceholder(getImageBitmap) }
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ImageOrPlaceholder(
    getImageBitmap: suspend () -> ImageBitmap?
) {
    val loadedState = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val bitmap = remember {
        coroutineScope.async {
            val bitmap = getImageBitmap()
            loadedState.value = true
            bitmap
        }
    }

    val bitmapNow = bitmap
        .takeIf { loadedState.value }
        ?.getCompleted()

    if (bitmapNow != null) {
        Image(bitmapNow)
    } else {
        Placeholder()
    }
}

@Composable
private fun Placeholder() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Gray)
    )
}
