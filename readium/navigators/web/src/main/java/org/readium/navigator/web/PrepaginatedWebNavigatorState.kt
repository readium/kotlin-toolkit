package org.readium.navigator.web

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.web.layout.LayoutResolver
import org.readium.navigator.web.layout.Page
import org.readium.navigator.web.layout.Spread
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorDefaults
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorPreferences
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorSettings
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorSettingsResolver
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
@Stable
public class PrepaginatedWebNavigatorState internal constructor(
    publicationMetadata: Metadata,
    private val readingOrder: ReadingOrder,
    initialPreferences: PrepaginatedWebNavigatorPreferences,
    defaults: PrepaginatedWebNavigatorDefaults,
    initialItem: Int,
    internal val webViewServer: WebViewServer,
    internal val preloadedData: PreloadedData
) {

    init {
        require(initialItem < readingOrder.items.size)
    }

    internal data class PreloadedData(
        val prepaginatedSingleContent: String,
        val prepaginatedDoubleContent: String
    )
    public data class ReadingOrder(
        val items: List<Item>
    ) {

        public data class Item(
            val href: Url,
            val page: Presentation.Page?
        )
    }

    public data class Location(
        val href: Url
    )

    private val settingsResolver =
        PrepaginatedWebNavigatorSettingsResolver(publicationMetadata, defaults)

    private val layoutResolver =
        LayoutResolver(readingOrder.items.map { Page(it.href, it.page) })

    public val preferences: MutableState<PrepaginatedWebNavigatorPreferences> =
        mutableStateOf(initialPreferences)

    public val settings: State<PrepaginatedWebNavigatorSettings> =
        derivedStateOf { settingsResolver.settings(preferences.value) }

    internal val webViewClient =
        WebViewClient(webViewServer)

    internal val spreads: State<List<Spread>> =
        derivedStateOf { layoutResolver.layout(settings.value) }

    internal val fit: State<Fit> =
        derivedStateOf { settings.value.fit }

    internal val pagerState: PagerState =
        PagerState(currentPage = spreadIndexOfItem(initialItem)) { spreads.value.size }

    public val currentItem: State<Int> =
        derivedStateOf { itemIndexOfSpread(spreads.value[pagerState.currentPage]) }

    private fun spreadIndexOfItem(item: Int): Int {
        val href = readingOrder.items[item].href
        return spreads.value.indexOfFirst { it.contains(href) }
    }

    @OptIn(DelicateReadiumApi::class)
    private fun itemIndexOfSpread(spread: Spread): Int =
        when (spread) {
            is Spread.Single ->
                readingOrder.items
                    .indexOfFirst { spread.page == it.href }
            is Spread.Double ->
                readingOrder.items
                    .indexOfFirst { spread.leftPage == it.href }
                    .takeUnless { it == -1 }
                    ?: readingOrder.items
                        .indexOfFirst { spread.rightPage == it.href }
        }

    internal fun spreadKey(spread: Spread): Any =
        when (spread) {
            is Spread.Double -> when (settings.value.readingProgression) {
                ReadingProgression.LTR -> spread.leftPage ?: spread.rightPage!!
                ReadingProgression.RTL -> spread.rightPage ?: spread.leftPage!!
            }
            is Spread.Single -> spread.page
        }
}
