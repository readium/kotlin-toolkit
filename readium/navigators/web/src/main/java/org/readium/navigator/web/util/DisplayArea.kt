package org.readium.navigator.web.util

import androidx.compose.ui.geometry.Size

internal data class DisplayArea(
    val viewportSize: Size,
    val safeDrawingPadding: AbsolutePaddingValues
)

internal data class AbsolutePaddingValues(
    val top: Int,
    val right: Int,
    val bottom: Int,
    val left: Int
)
