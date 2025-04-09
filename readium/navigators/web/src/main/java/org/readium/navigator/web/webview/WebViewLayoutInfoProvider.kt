package org.readium.navigator.web.webview

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.readium.navigator.web.snapping.PagingLayoutInfo

internal class WebViewLayoutInfoProvider(
    override val density: Density,
    override val orientation: Orientation,
    override val reverseLayout: Boolean,
    private val viewportSize: DpSize,
    private val webView: RelaxedWebView? = null,
) : PagingLayoutInfo {

    override val positionThresholdFraction: Float get() =
        with(density) {
            val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
            minThreshold / pageSize.toFloat()
        }

    override val pageSize: Int =
        with(density) { viewportSize.width.toPx().roundToInt() }

    override val pageSpacing: Int = 0

    override val upDownDifference: Offset get() =
        Offset.Zero // FIXME

    val pageCount: Int get() =
        ceil(webView!!.width / pageSize.toDouble()).toInt()

    val firstVisiblePage: Int get() =
        webView!!.scrollX * pageCount / webView.width

    override val visiblePageOffsets: List<Int>
        get() = buildList<Int> {
            // two pages are visible
            // the first one starts at firstVisiblePage * pageSize in the Webview document coordinates
            val firstVisibleOffset = (firstVisiblePage * pageSize - webView!!.scrollX)
            add((firstVisibleOffset + pageSize))
            add(firstVisibleOffset)
        }

    override val canScrollForward: Boolean
        get() = when {
            orientation == Orientation.Horizontal && reverseLayout ->
                webView!!.canScrollLeft
            orientation == Orientation.Horizontal ->
                webView!!.canScrollRight
            orientation == Orientation.Vertical && reverseLayout ->
                webView!!.canScrollTop
            else ->
                webView!!.canScrollBottom
        }

    override val canScrollBackward: Boolean
        get() = when {
            orientation == Orientation.Horizontal && reverseLayout ->
                webView!!.canScrollRight
            orientation == Orientation.Horizontal ->
                webView!!.canScrollLeft
            orientation == Orientation.Vertical && reverseLayout ->
                webView!!.canScrollBottom
            else ->
                webView!!.canScrollTop
        }
}

internal val DefaultPositionThreshold = 56.dp
