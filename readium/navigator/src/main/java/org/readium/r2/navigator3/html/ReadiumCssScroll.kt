package org.readium.r2.navigator3.html

import kotlin.math.roundToInt

internal class ReadiumCssScroll(
    private val webView: RelaxedWebView,
    private val scrollModeEnabled: Boolean,
    private val isRtl: Boolean
) {
    val progression: Double get() =
        if (scrollModeEnabled) {
            val y = webView.scrollY.toDouble()
            val contentHeight = webView.verticalScrollRange

            var progression = 0.0
            if (contentHeight > 0) {
                progression = (y / contentHeight).coerceIn(0.0, 1.0)
            }

            progression

        } else {
            var x = webView.scrollX.toDouble()
            val pageWidth = webView.horizontalScrollExtent
            val contentWidth = webView.horizontalScrollRange

            // For RTL, we need to add the equivalent of one page to the x position, otherwise the
            // progression will be one page off.
            if (isRtl) {
                x += pageWidth
            }

            var progression = 0.0
            if (contentWidth > 0) {
                progression = (x / contentWidth).coerceIn(0.0, 1.0)
            }
            // For RTL, we need to reverse the progression because the web view is always scrolling
            // from left to right, no matter the reading direction.
            if (isRtl) {
                progression = 1 - progression
            }

            progression
        }

    fun scrollToProgression(progression: Double) {
        require(progression in 0f..1f) { "Expected progression from 0.0 to  1.0." }

        if (scrollModeEnabled) {
            val offset = webView.verticalScrollRange * progression
            webView.scrollY = offset.roundToInt()
        } else {
            val documentWidth = webView.horizontalScrollRange
            val factor = if (isRtl) -1 else 1
            val offset = documentWidth * progression * factor
            webView.scrollX = snapOffset(offset.roundToInt())
        }
    }

    private fun snapOffset(offset: Int): Int {
        val value = offset + if (isRtl) -1 else 1
        return value + - (value % webView.horizontalScrollExtent);
    }
}
