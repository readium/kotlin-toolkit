package org.readium.navigator.web

import org.readium.navigator.web.preferences.NavigatorSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
internal class LayoutResolver(
    private val readingOrder: List<Page>
) {

    enum class Position {
        Left,
        Right,
        Center
    }

    data class Page(
        val url: Url
    )

    sealed class Spread {

        data class Single(
            val url: Url,
            val position: Position? = null
        ) : Spread()

        data class Double(
            val first: Url,
            val second: Url
        ) : Spread()
    }

    @Suppress("Unused_parameter")
    fun layout(settings: NavigatorSettings): List<Spread> =
        readingOrder.map { Spread.Single(it.url) }
}
