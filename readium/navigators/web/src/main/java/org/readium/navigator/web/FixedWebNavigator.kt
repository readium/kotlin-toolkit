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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.readium.navigator.web.layout.DoubleViewportSpread
import org.readium.navigator.web.layout.SingleViewportSpread
import org.readium.navigator.web.pager.NavigatorPager
import org.readium.navigator.web.spread.DoubleSpreadState
import org.readium.navigator.web.spread.DoubleViewportSpread
import org.readium.navigator.web.spread.SingleSpreadState
import org.readium.navigator.web.spread.SingleViewportSpread
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
@Composable
public fun FixedWebNavigator(
    modifier: Modifier = Modifier,
    displayCutout: WindowInsets = WindowInsets.displayCutout,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    onTap: (TapEvent) -> Unit = {},
    state: FixedWebNavigatorState
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val viewportSize = Size(maxWidth.value, maxHeight.value)

        val safeDrawingPadding = AbsolutePaddingValues(
            top = displayCutout.getTop(LocalDensity.current),
            right = displayCutout.getRight(LocalDensity.current, LocalLayoutDirection.current),
            bottom = displayCutout.getBottom(LocalDensity.current),
            left = displayCutout.getLeft(LocalDensity.current, LocalLayoutDirection.current)
        )

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
                        onTap = onTap,
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
                        onTap = onTap,
                        state = spreadState,
                        backgroundColor = backgroundColor
                    )
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
