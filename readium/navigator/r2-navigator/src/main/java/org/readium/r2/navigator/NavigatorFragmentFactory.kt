/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.readium.r2.navigator.cbz.ImageNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.pager.R2CbzPageFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.FragmentNavigator
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Factory for the Navigator Fragments.
 *
 * @param publication Publication to render in the navigator.
 * @param baseUrl A base URL where this publication is served from, when relevant. This is required
 *        only for an EPUB publication.
 * @param initialLocator The first location which should be visible when rendering the publication.
 *        Can be used to restore the last reading location.
 * @param listener Optional listener to implement to observe events, such as user taps.
 */
@FragmentNavigator
class NavigatorFragmentFactory(
    private val publication: Publication,
    private val baseUrl: String? = null,
    private val initialLocator: Locator? = null,
    private val listener: Navigator.Listener? = null
) : FragmentFactory() {

    @OptIn(PdfSupport::class)
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        when (className) {
            PdfNavigatorFragment::class.java.name ->
                PdfNavigatorFragment(publication, initialLocator, listener)

            EpubNavigatorFragment::class.java.name -> {
                val baseUrl = baseUrl ?: throw IllegalArgumentException("[baseUrl] is required for the [EpubNavigatorFragment]")
                EpubNavigatorFragment(publication, baseUrl, initialLocator, listener)
            }

            ImageNavigatorFragment::class.java.name ->
                ImageNavigatorFragment(publication, initialLocator, listener)

            R2CbzPageFragment::class.java.name ->
                R2CbzPageFragment(publication)

            else -> super.instantiate(classLoader, className)
        }

}