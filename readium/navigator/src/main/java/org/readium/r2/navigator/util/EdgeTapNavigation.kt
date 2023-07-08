package org.readium.r2.navigator.util

import org.readium.r2.navigator.VisualNavigator

@Deprecated("Replaced by [DirectionalNavigationAdapter].", replaceWith = ReplaceWith("DirectionalNavigationAdapter"), level = DeprecationLevel.ERROR)
class EdgeTapNavigation(
    private val navigator: VisualNavigator,
    private val minimumEdgeSize: Double = 200.0,
    private val edgeThresholdPercent: Double? = 0.3,
    private val animatedTransition: Boolean = false,
    private val handleTapsWhileScrolling: Boolean = false
)
