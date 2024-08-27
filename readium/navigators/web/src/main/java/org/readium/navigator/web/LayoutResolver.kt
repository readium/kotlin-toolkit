package org.readium.navigator.web

import org.readium.navigator.web.preferences.NavigatorSettings
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
internal class LayoutResolver(
    private val readingOrder: List<Page>
) {
    data class Page(
        val url: Url,
        val position: Position?
    )

    sealed class Spread {

        data class Single(
            val value: Url
        ) : Spread()

        data class Double(
            val left: Url?,
            val right: Url?
        ) : Spread()
    }

    fun layout(settings: NavigatorSettings): List<Spread> =
        if (settings.spreads) {
            when (settings.readingProgression) {
                ReadingProgression.LTR -> layoutSpreadsLtr()
                ReadingProgression.RTL -> layoutSpreadsRtl()
            }
        } else {
            readingOrder.map { Spread.Single(it.url) }
        }

    private fun layoutSpreadsLtr(): List<Spread> =
        buildList {
            var pending: Page? = null

            for (page in readingOrder) {
                when (page.position) {
                    Position.Left -> {
                        pending?.let { add(Spread.Double(it.url, null)) }
                        pending = page
                    }
                    Position.Right -> {
                        add(Spread.Double(pending?.url, page.url))
                        pending = null
                    }
                    null -> {
                        if (pending == null) {
                            pending = page
                        } else {
                            add(Spread.Double(pending.url, page.url))
                            pending = null
                        }
                    }
                }
            }

            pending?.let { add(Spread.Double(it.url, null)) }
        }

    private fun layoutSpreadsRtl(): List<Spread> =
        buildList {
            var pending: Page? = null

            for (page in readingOrder) {
                when (page.position) {
                    Position.Left -> {
                        add(Spread.Double(page.url, pending?.url))
                        pending = null
                    }
                    Position.Right -> {
                        pending?.let { add(Spread.Double(null, it.url)) }
                        pending = page
                    }
                    null -> {
                        if (pending == null) {
                            pending = page
                        } else {
                            add(Spread.Double(page.url, pending.url))
                            pending = null
                        }
                    }
                }
            }

            pending?.let { add(Spread.Double(null, it.url)) }
        }
}
