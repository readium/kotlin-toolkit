package org.readium.navigator.demo.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun Fullscreenable(
    fullscreenState: State<Boolean>,
    insetsController: WindowInsetsControllerCompat,
    content: @Composable () -> Unit
) {
    insetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    LaunchedEffect(fullscreenState.value) {
        if (fullscreenState.value) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    content.invoke()
}
