/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import androidx.fragment.app.FragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Factory of the Web navigator and related components.
 *
 * @param publication Web publication to render in the navigator.
 */
@ExperimentalReadiumApi
public class WebNavigatorFactory(
    private val publication: Publication
) {
    public fun createFragmentFactory(
        initialLocator: Locator?
    ): FragmentFactory = org.readium.r2.navigator.util.createFragmentFactory {
        WebNavigatorFragment(
            publication = publication,
            initialLocator = initialLocator
        )
    }
}
