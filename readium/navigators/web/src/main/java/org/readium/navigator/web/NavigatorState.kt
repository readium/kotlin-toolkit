package org.readium.navigator.web

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.readium.navigator.web.preferences.NavigatorDefaults
import org.readium.navigator.web.preferences.NavigatorPreferences
import org.readium.navigator.web.preferences.NavigatorSettings
import org.readium.navigator.web.preferences.NavigatorSettingsResolver
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
@Stable
@Suppress("Unused_parameter")
public class NavigatorState internal constructor(
    publicationMetadata: Metadata,
    readingOrder: ReadingOrder,
    initialPreferences: NavigatorPreferences,
    defaults: NavigatorDefaults,
    initialItem: Int,
    internal val webViewServer: WebViewServer,
    internal val fxlSpreadOne: String,
    internal val fxlSpreadTwo: String
) {
    public data class ReadingOrder(
        val items: List<Item>
    ) {

        public data class Item(
            val href: Url,
            val position: Position?
        )
    }

    public data class Location(
        val href: Url
    )

    private val settingsResolver =
        NavigatorSettingsResolver(publicationMetadata, defaults)

    private val layoutResolver =
        LayoutResolver(readingOrder.items.map { LayoutResolver.Page(it.href, it.position) })

    public val preferences: MutableState<NavigatorPreferences> =
        mutableStateOf(initialPreferences)

    public val settings: State<NavigatorSettings> =
        derivedStateOf { settingsResolver.settings(preferences.value) }

    internal val webViewClient =
        WebViewClient(webViewServer)

    internal val spreads: State<List<LayoutResolver.Spread>> =
        derivedStateOf { layoutResolver.layout(settings.value) }
}
