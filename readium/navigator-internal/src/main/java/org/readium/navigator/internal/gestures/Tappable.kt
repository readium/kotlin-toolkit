package org.readium.navigator.internal.gestures

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import kotlinx.coroutines.coroutineScope

fun Modifier.tappable(
    enabled: Boolean = true,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "tappable"
        properties["enabled"] = enabled
        properties["onDoubleTap"] = onDoubleTap
        properties["onTap"] = onTap
    }
) {
    this.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        coroutineScope {
            forEachGesture {
                detectTapGestures(
                    onDoubleTap = onDoubleTap,
                    onTap = onTap
                )
            }
        }
    }
}

