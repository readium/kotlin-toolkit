package org.readium.navigator.web

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import org.readium.navigator.common.HyperlinkListener
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.LinkContext
import org.readium.navigator.common.TapContext
import org.readium.navigator.common.defaultHyperlinkListener
import org.readium.navigator.common.defaultInputListener
import org.readium.navigator.web.layout.DoubleViewportSpread
import org.readium.navigator.web.layout.FixedWebReadingOrder
import org.readium.navigator.web.layout.SingleViewportSpread
import org.readium.navigator.web.pager.NavigatorPager
import org.readium.navigator.web.spread.DoubleSpreadState
import org.readium.navigator.web.spread.DoubleViewportSpread
import org.readium.navigator.web.spread.SingleSpreadState
import org.readium.navigator.web.spread.SingleViewportSpread
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
@Composable
public fun FixedWebNavigator(
    modifier: Modifier = Modifier,
    state: FixedWebNavigatorState,
    windowInsets: WindowInsets = WindowInsets.displayCutout,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    inputListener: InputListener = defaultInputListener(navigatorState = state),
    hyperlinkListener: HyperlinkListener = defaultHyperlinkListener(navigatorState = state)
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val viewportSize = DpSize(maxWidth, maxHeight)

        val safeDrawingPadding = windowInsets.asAbsolutePaddingValues()
        val displayArea = rememberUpdatedState(DisplayArea(viewportSize, safeDrawingPadding))

        val reverseLayout =
            LocalLayoutDirection.current.toReadingProgression() != state.settings.value.readingProgression

        NavigatorPager(
            modifier = modifier,
            state = state.pagerState,
            beyondViewportPageCount = 2,
            key = { index -> state.layout.value.pageIndexForSpread(index) },
            reverseLayout = reverseLayout
        ) { index ->
            when (val spread = state.layout.value.spreads[index]) {
                is SingleViewportSpread -> {
                    val spreadState = remember {
                        SingleSpreadState(
                            htmlData = state.preloadedData.prepaginatedSingleContent,
                            publicationBaseUrl = WebViewServer.publicationBaseHref,
                            webViewClient = state.webViewClient,
                            spread = spread,
                            fit = state.fit,
                            displayArea = displayArea
                        )
                    }

                    SingleViewportSpread(
                        onTap = { inputListener.onTap(it, TapContext(viewportSize)) },
                        onLinkActivated = { url, context ->
                            onLinkActivated(url, context, state.readingOrder, hyperlinkListener)
                        },
                        state = spreadState,
                        backgroundColor = backgroundColor
                    )
                }
                is DoubleViewportSpread -> {
                    val spreadState = remember {
                        DoubleSpreadState(
                            htmlData = state.preloadedData.prepaginatedDoubleContent,
                            publicationBaseUrl = WebViewServer.publicationBaseHref,
                            webViewClient = state.webViewClient,
                            spread = spread,
                            fit = state.fit,
                            displayArea = displayArea
                        )
                    }

                    DoubleViewportSpread(
                        onTap = { inputListener.onTap(it, TapContext(viewportSize)) },
                        onLinkActivated = { url, context ->
                            onLinkActivated(url, context, state.readingOrder, hyperlinkListener)
                        },
                        state = spreadState,
                        backgroundColor = backgroundColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowInsets.asAbsolutePaddingValues(): AbsolutePaddingValues {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val top = with(density) { getTop(density).toDp() }
    val right = with(density) { getRight(density, layoutDirection).toDp() }
    val bottom = with(density) { getBottom(density).toDp() }
    val left = with(density) { getLeft(density, layoutDirection).toDp() }
    return AbsolutePaddingValues(top = top, right = right, bottom = bottom, left = left)
}

@OptIn(ExperimentalReadiumApi::class)
private fun LayoutDirection.toReadingProgression(): ReadingProgression =
    when (this) {
        LayoutDirection.Ltr -> ReadingProgression.LTR
        LayoutDirection.Rtl -> ReadingProgression.RTL
    }

@OptIn(ExperimentalReadiumApi::class)
private fun onLinkActivated(
    url: Url,
    context: LinkContext?,
    readingOrder: FixedWebReadingOrder,
    listener: HyperlinkListener
) {
    readingOrder.indexOfHref(url.removeFragment())
        ?.let { listener.onReadingOrderLinkActivated(url, context) }
        ?: run {
            when (url) {
                is RelativeUrl -> listener.onResourceLinkActivated(url, context)
                is AbsoluteUrl -> listener.onExternalLinkActivated(url, context)
            }
        }
}
