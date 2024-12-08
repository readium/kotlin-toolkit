package org.readium.navigator.web.webview

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.readium.navigator.web.snapping.PagingLayoutInfo
import org.readium.navigator.web.util.DisplayArea

internal class WebViewLayoutInfoProvider(
    override val density: Density,
    override val orientation: Orientation,
    override val reverseLayout: Boolean,
    private val displayArea: DisplayArea,
    private val webViewScrollState: WebViewScrollable2DState,
    private val webView: RelaxedWebView,
) : PagingLayoutInfo {

    override val positionThresholdFraction: Float get() =
        with(density) {
            val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
            minThreshold / pageSize.toFloat()
        }

    override val pageSize: Int =
        with(density) { displayArea.viewportSize.width.toPx().roundToInt() }

    override val pageSpacing: Int = 0

    override val upDownDifference: Offset get() =
        Offset.Zero // FIXME

    override val pageCount: Int get() =
        ceil(webView.width / pageSize.toDouble()).toInt()

    override val firstVisiblePage: Int get() =
        webView.scrollX * pageCount / webView.width

    override val visiblePageOffsets: List<Float>
        get() = buildList<Float> {
            // two pages are visible
            // the first one starts at firstVisiblePage * pageSize in the Webview document coordinates
            val firstVisibleOffset = (firstVisiblePage * pageSize - webView.scrollX).toFloat()
            add((firstVisibleOffset + pageSize))
            add(firstVisibleOffset)
        }

    override val canScrollForward: Boolean
        get() = if (reverseLayout) webView.canScrollLeft else webView.canScrollRight

    override val canScrollBackward: Boolean
        get() = if (reverseLayout) webView.canScrollRight else webView.canScrollLeft
}

internal val DefaultPositionThreshold = 56.dp
