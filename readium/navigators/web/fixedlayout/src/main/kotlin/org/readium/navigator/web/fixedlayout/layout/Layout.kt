/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.layout

import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

internal data class Layout(
    val readingProgression: ReadingProgression,
    val spreads: List<Spread>,
) {

    fun spreadIndexForHref(href: Url): Int? = spreads
        .indexOfFirst { href in it.pages.map { page -> page.href } }
        .takeUnless { it == -1 }

    fun spreadIndexForPage(pageIndex: Int): Int = spreads
        .indexOfFirst { pageIndex in it.pages.map { page -> page.index } }
        .also { check(it != -1) }

    fun pageIndexForSpread(spreadIndex: Int) =
        when (val spread = spreads[spreadIndex]) {
            is SingleViewportSpread ->
                spread.page.index
            is LeftOnlySpread ->
                spread.page.index
            is RightOnlySpread ->
                spread.page.index
            is DoubleSpread ->
                when (readingProgression) {
                    ReadingProgression.LTR -> spread.leftPage.index
                    ReadingProgression.RTL -> spread.rightPage.index
                }
        }
}

internal data class Page(
    val index: Int,
    val href: Url,
    val mediaType: MediaType?,
)

internal sealed interface Spread {

    val pages: List<Page>

    fun contains(href: Url): Boolean =
        href in pages.map { it.href }
}

internal data class SingleViewportSpread(
    val page: Page,
) : Spread {

    override val pages: List<Page> get() = listOfNotNull(page)
}

internal sealed class DoubleViewportSpread(
    override val pages: List<Page>,
) : Spread {
    abstract val leftPage: Page?

    abstract val rightPage: Page?

    companion object {

        operator fun invoke(leftPage: Page?, rightPage: Page?): Spread =
            when {
                leftPage != null && rightPage != null -> DoubleSpread(leftPage, rightPage)
                leftPage != null -> LeftOnlySpread(leftPage)
                rightPage != null -> RightOnlySpread(rightPage)
                else -> throw IllegalArgumentException("Attempt to create an empty spread.")
            }
    }
}

internal data class LeftOnlySpread(
    val page: Page,
) : DoubleViewportSpread(listOf(page)) {

    override val leftPage: Page = page

    override val rightPage: Page? = null
}

internal data class RightOnlySpread(
    val page: Page,
) : DoubleViewportSpread(listOf(page)) {

    override val leftPage: Page? = null

    override val rightPage: Page = page
}

internal data class DoubleSpread(
    override val leftPage: Page,
    override val rightPage: Page,
) : DoubleViewportSpread(listOf(leftPage, rightPage))
