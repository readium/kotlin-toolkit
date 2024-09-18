package org.readium.navigator.web.layout

import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorSettings
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.presentation.Presentation

@OptIn(ExperimentalReadiumApi::class)
internal class LayoutResolver(
    private val readingOrder: ReadingOrder
) {

    fun layout(settings: PrepaginatedWebNavigatorSettings): List<Spread> =
        if (settings.spreads) {
            when (settings.readingProgression) {
                ReadingProgression.LTR -> layoutSpreadsLtr()
                ReadingProgression.RTL -> layoutSpreadsRtl()
            }
        } else {
            readingOrder.items.mapIndexed { index, item ->
                SingleViewportSpread(
                    Page(index, item.href)
                )
            }
        }

    private fun layoutSpreadsLtr(): List<Spread> =
        buildList {
            var pending: Page? = null

            for ((index, item) in readingOrder.items.withIndex()) {
                val page = Page(index, item.href)

                when (item.page) {
                    Presentation.Page.LEFT -> {
                        pending?.let { add(LeftOnlySpread(it)) }
                        pending = page
                    }
                    Presentation.Page.RIGHT -> {
                        add(DoubleViewportSpread(pending, page))
                        pending = null
                    }
                    Presentation.Page.CENTER -> {
                        pending?.let { add(LeftOnlySpread(it)) }
                        pending = null
                        add(SingleViewportSpread(page))
                    }
                    null -> {
                        if (pending == null) {
                            pending = page
                        } else {
                            add(DoubleSpread(pending, page))
                            pending = null
                        }
                    }
                }
            }

            pending?.let { add(LeftOnlySpread(it)) }
        }

    private fun layoutSpreadsRtl(): List<Spread> =
        buildList {
            var pending: Page? = null

            for ((index, item) in readingOrder.items.withIndex()) {
                val page = Page(index, item.href)

                when (item.page) {
                    Presentation.Page.LEFT -> {
                        add(DoubleViewportSpread(page, pending))
                        pending = null
                    }
                    Presentation.Page.RIGHT -> {
                        pending?.let { add(RightOnlySpread(it)) }
                        pending = page
                    }
                    Presentation.Page.CENTER -> {
                        pending?.let { add(RightOnlySpread(it)) }
                        pending = null
                        add(SingleViewportSpread(page))
                    }
                    null -> {
                        if (pending == null) {
                            pending = page
                        } else {
                            add(DoubleSpread(page, pending))
                            pending = null
                        }
                    }
                }
            }

            pending?.let { add(RightOnlySpread(it)) }
        }
}
