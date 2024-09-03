package org.readium.navigator.web.layout

import org.readium.navigator.web.preferences.NavigatorSettings
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation

@ExperimentalReadiumApi
internal class LayoutResolver(
    private val readingOrder: List<Page>
) {

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
                when (page.page) {
                    Presentation.Page.LEFT -> {
                        pending?.let { add(Spread.Double(it.url, null)) }
                        pending = page
                    }
                    Presentation.Page.RIGHT -> {
                        add(Spread.Double(pending?.url, page.url))
                        pending = null
                    }
                    Presentation.Page.CENTER -> {
                        pending?.let { add(Spread.Double(it.url, null)) }
                        pending = null
                        add(Spread.Single(page.url))
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
                when (page.page) {
                    Presentation.Page.LEFT -> {
                        add(Spread.Double(page.url, pending?.url))
                        pending = null
                    }
                    Presentation.Page.RIGHT -> {
                        pending?.let { add(Spread.Double(null, it.url)) }
                        pending = page
                    }
                    Presentation.Page.CENTER -> {
                        pending?.let { add(Spread.Double(null, it.url)) }
                        pending = null
                        add(Spread.Single(page.url))
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
