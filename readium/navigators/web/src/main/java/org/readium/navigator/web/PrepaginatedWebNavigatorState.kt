package org.readium.navigator.web

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.web.layout.Layout
import org.readium.navigator.web.layout.LayoutResolver
import org.readium.navigator.web.layout.ReadingOrder
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorDefaults
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorPreferences
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorSettings
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorSettingsResolver
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata

@ExperimentalReadiumApi
@Stable
public class PrepaginatedWebNavigatorState internal constructor(
    publicationMetadata: Metadata,
    readingOrder: ReadingOrder,
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

    private val settingsResolver =
        PrepaginatedWebNavigatorSettingsResolver(publicationMetadata, defaults)

    private val layoutResolver =
        LayoutResolver(readingOrder)

    public val preferences: MutableState<PrepaginatedWebNavigatorPreferences> =
        mutableStateOf(initialPreferences)

    public val settings: State<PrepaginatedWebNavigatorSettings> =
        derivedStateOf { settingsResolver.settings(preferences.value) }

    internal val webViewClient =
        WebViewClient(webViewServer)

    internal val layout: State<Layout> =
        derivedStateOf {
            val spreads = layoutResolver.layout(settings.value)
            Layout(settings.value.readingProgression, spreads)
        }

    internal val fit: State<Fit> =
        derivedStateOf { settings.value.fit }

    internal val pagerState: PagerState =
        PagerState(currentPage = layout.value.spreadIndexForPage(initialItem)) { layout.value.spreads.size }

    public val currentItem: State<Int> =
        derivedStateOf { layout.value.pageIndexForSpread(pagerState.currentPage) }
}
