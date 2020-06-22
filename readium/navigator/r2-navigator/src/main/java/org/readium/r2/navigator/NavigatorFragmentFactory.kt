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
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class NavigatorFragmentFactory(
    private val publication: Publication,
    private val initialLocator: Locator? = null,
    private val listener: Navigator.Listener? = null
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        when (className) {
            PdfNavigatorFragment::class.java.name -> PdfNavigatorFragment(publication, initialLocator, listener)
            else -> super.instantiate(classLoader, className)
        }

}