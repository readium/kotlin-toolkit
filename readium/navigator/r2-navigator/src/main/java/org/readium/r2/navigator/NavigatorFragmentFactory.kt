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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorViewModel
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class NavigatorFragmentFactory(
    private val publication: Publication,
    private val initialLocator: Locator? = null
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        when (className) {
            PdfNavigatorFragment::class.java.name -> PdfNavigatorFragment(object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    if (!modelClass.isAssignableFrom(PdfNavigatorViewModel::class.java)) {
                        throw ClassCastException("Unknown ViewModel class: ${modelClass.name}")
                    }
                    return PdfNavigatorViewModel(publication, initialLocator) as T
                }
            })
            else -> super.instantiate(classLoader, className)
        }

}