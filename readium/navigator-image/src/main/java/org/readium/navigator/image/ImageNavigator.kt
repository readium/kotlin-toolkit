package org.readium.navigator.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import org.readium.navigator.image.viewer.ImageViewer
import org.readium.navigator.internal.gestures.tappable
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
@Composable
fun ImageNavigator(
    modifier: Modifier,
    state: ImageNavigatorState,
    onTap: ((Offset) -> Unit)?
) {
    ImageViewer(
        modifier = modifier
            .tappable(true, onTap = onTap),
        state = state.viewerState
    )
}
