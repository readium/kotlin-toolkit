package org.readium.r2.navigator3.image

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.navigator3.Overflow
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@Composable
fun SingleImageResource(
    publication: Publication,
    link: Link,
    scaleState: MutableState<Float>,
    overflow: Overflow
) {
    val bitmap = runBlocking {
        val bytes = publication.get(link).read().getOrThrow()
        BitmapFactory.decodeByteArray(bytes)!!
    }

    if (overflow == Overflow.PAGINATED) {
        ScrollableImage(
            bitmap.asImageBitmap(),
            scaleState
        )
    } else {
        Image(
            bitmap.asImageBitmap(),
            scaleState.value
        )
    }
}

@Composable
private fun ScrollableImage(
    bitmap: ImageBitmap,
    scaleState: MutableState<Float>
) {
    val state = rememberZoomableBoxState(scaleState)

    ZoomableBox(
        modifier = Modifier.fillMaxSize(),
        state = state
    ) {
        Image(bitmap, scaleState.value)
    }
}
