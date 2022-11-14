package org.readium.navigator.internal.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import timber.log.Timber

fun Modifier.logConstraints(key: String): Modifier =
    this.layout { measurable, constraints ->
        Timber.d("$key ${constraints.minWidth} ${constraints.minHeight} ${constraints.maxWidth} ${constraints.maxHeight}")
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
