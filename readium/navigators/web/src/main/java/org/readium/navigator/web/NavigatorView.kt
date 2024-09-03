package org.readium.navigator.web

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.readium.navigator.web.logging.LoggingNestedScrollConnection
import org.readium.navigator.web.logging.LoggingTargetedFlingBehavior
import org.readium.navigator.web.spread.DoubleSpread
import org.readium.navigator.web.spread.DoubleSpreadState
import org.readium.navigator.web.spread.SingleSpread
import org.readium.navigator.web.spread.SingleSpreadState
import org.readium.navigator.web.util.PagerNestedConnection
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
@Composable
public fun NavigatorView(
    modifier: Modifier,
    state: NavigatorState
) {
    val pagerState = rememberPagerState {
        state.spreads.value.size
    }

    val flingBehavior = LoggingTargetedFlingBehavior(
        PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(0)
        )
    )

    val reverseLayout =
        LocalLayoutDirection.current.toReadingProgression() != state.settings.value.readingProgression

    HorizontalPager(
        modifier = modifier,
        userScrollEnabled = false,
        state = pagerState,
        beyondViewportPageCount = 2,
        reverseLayout = reverseLayout,
        flingBehavior = flingBehavior,
        pageNestedScrollConnection = LoggingNestedScrollConnection(
            PagerNestedConnection(pagerState, flingBehavior, Orientation.Horizontal)
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            propagateMinConstraints = true
        ) {
            val viewportState = rememberUpdatedState(Size(maxWidth.value, maxHeight.value))

            when (val spread = state.spreads.value[it]) {
                is LayoutResolver.Spread.Single -> {
                    val spreadState = remember {
                        SingleSpreadState(
                            htmlData = state.fxlSpreadOne,
                            publicationBaseUrl = WebViewServer.publicationBaseHref,
                            webViewClient = state.webViewClient,
                            spread = spread,
                            fit = state.fit,
                            viewport = viewportState
                        )
                    }

                    SingleSpread(state = spreadState)
                }
                is LayoutResolver.Spread.Double -> {
                    val spreadState = remember {
                        DoubleSpreadState(
                            htmlData = state.fxlSpreadTwo,
                            publicationBaseUrl = WebViewServer.publicationBaseHref,
                            webViewClient = state.webViewClient,
                            spread = spread,
                            fit = state.fit,
                            viewport = viewportState
                        )
                    }

                    DoubleSpread(state = spreadState)
                }
            }
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun LayoutDirection.toReadingProgression(): ReadingProgression =
    when (this) {
        LayoutDirection.Ltr -> ReadingProgression.LTR
        LayoutDirection.Rtl -> ReadingProgression.RTL
    }
