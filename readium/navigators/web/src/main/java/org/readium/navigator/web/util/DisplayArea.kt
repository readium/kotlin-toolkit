package org.readium.navigator.web.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

internal data class DisplayArea(
    val viewportSize: DpSize,
    val safeDrawingPadding: AbsolutePaddingValues
)

internal data class AbsolutePaddingValues(
    val top: Dp,
    val right: Dp,
    val bottom: Dp,
    val left: Dp
)
