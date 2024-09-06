package org.readium.navigator.web

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.readium.navigator.web.layout.Spread
import org.readium.navigator.web.pager.NavigatorPager
import org.readium.navigator.web.spread.DoubleSpread
import org.readium.navigator.web.spread.DoubleSpreadState
import org.readium.navigator.web.spread.SingleSpread
import org.readium.navigator.web.spread.SingleSpreadState
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import timber.log.Timber

@ExperimentalReadiumApi
@Composable
public fun PrepaginatedWebNavigator(
    modifier: Modifier,
    state: PrepaginatedWebNavigatorState
) {
    val pagerState = rememberPagerState {
        state.spreads.value.size
    }

    val reverseLayout =
        LocalLayoutDirection.current.toReadingProgression() != state.settings.value.readingProgression

    NavigatorPager(
        modifier = modifier,
        state = pagerState,
        beyondViewportPageCount = 2,
        reverseLayout = reverseLayout
    ) { index ->

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            propagateMinConstraints = true
        ) {
            val viewportState = rememberUpdatedState(Size(maxWidth.value, maxHeight.value))

            // For some reason, index can be higher than the value of state.spreads.value.size
            // at this point. Try to skip composition of those extra items.
            if (index >= state.spreads.value.size) {
                Timber.e("Compose bug")
                return@BoxWithConstraints
            }

            when (val spread = state.spreads.value[index]) {
                is Spread.Single -> {
                    val spreadState = remember {
                        SingleSpreadState(
                            htmlData = state.preloadedData.prepaginatedSingleContent,
                            publicationBaseUrl = WebViewServer.publicationBaseHref,
                            webViewClient = state.webViewClient,
                            spread = spread,
                            fit = state.fit,
                            viewport = viewportState
                        )
                    }

                    SingleSpread(state = spreadState)
                }
                is Spread.Double -> {
                    val spreadState = remember {
                        DoubleSpreadState(
                            htmlData = state.preloadedData.prepaginatedDoubleContent,
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
